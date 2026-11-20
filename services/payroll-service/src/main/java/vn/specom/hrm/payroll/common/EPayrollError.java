package vn.specom.hrm.payroll.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum EPayrollError implements ErrorCode {
    PAY_001("Choose the company to act on."),
    PAY_002("The salary component does not exist."),
    PAY_003("This name already exists."),
    PAY_004("A percentage must be between 0 and 100."),
    PAY_005("It is still in use and cannot be deleted.");

    private final String message;
}
