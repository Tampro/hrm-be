package vn.specom.hrm.attendance.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum EAttendanceError implements ErrorCode {
    ATT_001("The shift does not exist."),
    ATT_002("The attendance policy does not exist."),
    ATT_003("This name already exists."),
    ATT_004("Choose the company to act on."),
    ATT_005("The end time must be after the start time, unless it is a night shift."),
    ATT_006("Give both the break start and the break end, with the end after the start."),
    ATT_007("The break cannot be longer than the shift.");

    private final String message;
}
