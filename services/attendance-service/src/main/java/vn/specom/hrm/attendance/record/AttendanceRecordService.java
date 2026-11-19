package vn.specom.hrm.attendance.record;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.attendance.client.EmployeeClient;
import vn.specom.hrm.attendance.client.EmployeeClient.Employee;
import vn.specom.hrm.attendance.client.EmployeeClient.EmployeePage;
import vn.specom.hrm.attendance.client.EmployeeClient.Holiday;
import vn.specom.hrm.attendance.common.EAttendanceError;
import vn.specom.hrm.attendance.common.Texts;
import vn.specom.hrm.attendance.policy.AttendancePolicy;
import vn.specom.hrm.attendance.policy.AttendancePolicyRepository;
import vn.specom.hrm.attendance.record.RecordDtos.Calendar;
import vn.specom.hrm.attendance.record.RecordDtos.DayCell;
import vn.specom.hrm.attendance.record.RecordDtos.EmployeeRow;
import vn.specom.hrm.attendance.record.RecordDtos.RecordDto;
import vn.specom.hrm.attendance.record.RecordDtos.RecordForm;
import vn.specom.hrm.attendance.setting.AttendanceSettingService;
import vn.specom.hrm.attendance.shift.Shift;
import vn.specom.hrm.attendance.shift.ShiftRepository;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.security.TenantGuard;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Attendance Records page (docs/analysis/06 §6.4.1): month calendar, add/edit/delete a record. Employees and holidays
 * come from employee-service with the caller's token. A day without a record is Not Added; a record without clock-in
 * is Absent (PO, 2026-11-19); nothing is marked automatically.
 */
@Service
@RequiredArgsConstructor
public class AttendanceRecordService {
    /** TODO(assumption): "today" (Future cells, no clock times ahead) is the Vietnam date until companies carry a zone. */
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AttendanceRecordRepository records;
    private final ShiftRepository shifts;
    private final AttendancePolicyRepository policies;
    private final AttendanceSettingService settings;
    private final EmployeeClient employees;
    private final Clock clock;

    // ------------------------------------------------------------------ calendar

    @Transactional(readOnly = true)
    public Calendar calendar(Long requestedCompanyId, int year, int month, Long employeeId, int page, int size) {
        Long companyId = company(requestedCompanyId);
        YearMonth ym = YearMonth.of(year, month);
        List<Employee> rows;
        long total;
        if (employeeId != null) {
            rows = List.of(employees.get(employeeId));
            total = 1;
        } else {
            EmployeePage p = employees.page(requestedCompanyId, page, size);
            rows = p.content();
            total = p.totalElements();
        }
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        Set<DayOfWeek> off = settings.offDays(companyId);
        List<Holiday> holidays = employees.holidays(requestedCompanyId, from, to);
        Map<Long, Map<LocalDate, AttendanceRecord>> byEmployee = rows.isEmpty() ? Map.of()
                : records.findByCompanyIdAndEmployeeIdInAndWorkDateBetween(companyId,
                        rows.stream().map(e -> Long.valueOf(e.id())).toList(), from, to).stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getEmployeeId,
                        Collectors.toMap(AttendanceRecord::getWorkDate, Function.identity())));
        LocalDate today = today();
        List<EmployeeRow> out = new ArrayList<>();
        for (Employee e : rows) {
            Map<LocalDate, AttendanceRecord> mine = byEmployee.getOrDefault(Long.valueOf(e.id()), Map.of());
            out.add(row(e, ym, off, holidays, mine, today));
        }
        return new Calendar(year, month, today, off, out, page, size, total);
    }

    static EmployeeRow row(Employee e, YearMonth ym, Set<DayOfWeek> off, List<Holiday> holidays,
                           Map<LocalDate, AttendanceRecord> records, LocalDate today) {
        List<DayCell> days = new ArrayList<>();
        BigDecimal attended = BigDecimal.ZERO;
        int working = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            boolean weekend = off.contains(date.getDayOfWeek());
            boolean holiday = holidays.stream().anyMatch(h -> !date.isBefore(h.startDate()) && !date.isAfter(h.endDate())
                    && h.appliesTo(e.branchIdOrNull()));
            if (!weekend && !holiday) {
                working++;
            }
            AttendanceRecord r = records.get(date);
            if (r != null) {
                EDayStatus s = EDayStatus.valueOf(r.getStatus().name());
                if (s == EDayStatus.PRESENT) {
                    attended = attended.add(BigDecimal.ONE);
                } else if (s == EDayStatus.HALF_DAY) {
                    attended = attended.add(new BigDecimal("0.5"));
                }
                days.add(new DayCell(date, s, weekend, r.isLate(), r.isEarlyDeparture(),
                        r.getOvertimeHours().signum() > 0, r.getClockIn(), r.getClockOut(), r.getNotes(),
                        r.getId().toString()));
                continue;
            }
            EDayStatus s = holiday ? EDayStatus.HOLIDAY : weekend ? EDayStatus.DAY_OFF
                    : date.isAfter(today) ? EDayStatus.FUTURE : EDayStatus.NOT_ADDED;
            days.add(new DayCell(date, s, weekend, false, false, false, null, null, null, null));
        }
        return new EmployeeRow(e.id(), e.fullName(), e.employeeCode() != null ? e.employeeCode() : e.employeeNo(),
                e.designation() == null ? null : e.designation().name(), e.profileImageUrl(), days, attended, working);
    }

    // ------------------------------------------------------------------ records

    @Transactional(readOnly = true)
    public RecordDto get(Long id) {
        return RecordDto.of(require(id));
    }

    @Transactional
    public RecordDto create(RecordForm form) {
        Employee e = employees.get(form.employeeId());
        return RecordDto.of(save(new AttendanceRecord(), companyOf(e), e, form));
    }

    /** Full replacement: omitted times and notes are cleared; the shift and policy are read again from the profile. */
    @Transactional
    public RecordDto update(Long id, RecordForm form) {
        AttendanceRecord r = require(id);
        Employee e = employees.get(form.employeeId());
        return RecordDto.of(save(r, companyOf(e), e, form));
    }

    @Transactional
    public void delete(Long id) {
        records.delete(require(id));
    }

    /**
     * Saves a record of an employee of the company (callers resolve the company: the employee profile for one record,
     * the caller's company for an import).
     */
    AttendanceRecord save(AttendanceRecord r, Long companyId, Employee e, RecordForm f) {
        Long employeeId = Long.valueOf(e.id());
        if (f.clockOut() != null && f.clockIn() == null) {
            throw ApiException.badRequest(EAttendanceError.ATT_010);
        }
        if (f.clockIn() != null && f.date().isAfter(today())) {
            throw ApiException.badRequest(EAttendanceError.ATT_011);
        }
        if (records.existsByEmployeeIdAndWorkDateAndIdNot(employeeId, f.date(), r.getId() == null ? -1L : r.getId())) {
            throw ApiException.badRequest(EAttendanceError.ATT_009);
        }
        r.setCompanyId(companyId);
        r.setEmployeeId(employeeId);
        r.setWorkDate(f.date());
        r.setClockIn(f.clockIn());
        r.setClockOut(f.clockOut());
        r.setHoliday(f.holiday());
        r.setNotes(Texts.blankToNull(f.notes()));
        r.setWeekend(settings.offDays(companyId).contains(f.date().getDayOfWeek()));
        Shift shift = ownShift(companyId, e.shiftId());
        AttendancePolicy policy = ownPolicy(companyId, e.attendancePolicyId());
        r.setShiftId(shift == null ? null : shift.getId());
        r.setAttendancePolicyId(policy == null ? null : policy.getId());
        AttendanceCalculator.apply(r, shift, policy);
        return records.save(r);
    }

    /**
     * The employee's own company (from its profile). employee-service already refused another company's employee; the
     * check is repeated here so a record can never be written for a company the caller does not act for.
     */
    static Long companyOf(Employee e) {
        Long companyId = Long.valueOf(e.companyId());
        TenantGuard.assertSameCompany(companyId, EAttendanceError.ATT_008);
        return companyId;
    }

    private Shift ownShift(Long companyId, String id) {
        return id == null ? null : shifts.findById(Long.valueOf(id))
                .filter(s -> Objects.equals(s.getCompanyId(), companyId)).orElse(null);
    }

    private AttendancePolicy ownPolicy(Long companyId, String id) {
        return id == null ? null : policies.findById(Long.valueOf(id))
                .filter(p -> Objects.equals(p.getCompanyId(), companyId)).orElse(null);
    }

    AttendanceRecord require(Long id) {
        AttendanceRecord r = records.findById(id).orElseThrow(() -> ApiException.notFound(EAttendanceError.ATT_008));
        TenantGuard.assertSameCompany(r.getCompanyId(), EAttendanceError.ATT_008);
        return r;
    }

    LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }

    static Long company(Long requested) {
        return TenantGuard.resolveCompany(requested, EAttendanceError.ATT_004);
    }
}
