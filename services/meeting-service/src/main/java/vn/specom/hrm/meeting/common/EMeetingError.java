package vn.specom.hrm.meeting.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum EMeetingError implements ErrorCode {
    MTG_001("Choose the company to act on."),
    MTG_002("The meeting type does not exist."),
    MTG_003("This name already exists."),
    MTG_004("It is still in use and cannot be deleted."),
    MTG_005("The meeting room does not exist."),
    MTG_006("The employee service is unavailable. Please try again.");

    private final String message;
}
