package vn.specom.hrm.attendance.record;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByCompanyIdAndEmployeeIdInAndWorkDateBetween(Long companyId, Collection<Long> employeeIds,
                                                                           LocalDate from, LocalDate to);

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDateAndIdNot(Long employeeId, LocalDate workDate, Long id);
}
