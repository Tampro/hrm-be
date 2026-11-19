package vn.specom.hrm.recruitment.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum ERecruitmentError implements ErrorCode {
    REC_001("The item does not exist."),
    REC_002("The job location does not exist."),
    REC_003("The custom question does not exist."),
    REC_004("This name already exists."),
    REC_005("Choose the company to act on.");

    private final String message;
}
