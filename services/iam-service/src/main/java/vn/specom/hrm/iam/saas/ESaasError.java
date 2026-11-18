package vn.specom.hrm.iam.saas;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE keys on it. */
@Getter
@RequiredArgsConstructor
public enum ESaasError implements ErrorCode {
    SAAS_001("The plan does not exist."),
    SAAS_002("The plan code is already taken."),
    SAAS_003("This plan has no yearly price."),
    SAAS_004("The coupon does not exist."),
    SAAS_005("The coupon code is already taken."),
    SAAS_006("The coupon is invalid, expired or used up."),
    SAAS_007("The order total is below the coupon minimum spend."),
    SAAS_008("The order does not exist."),
    SAAS_009("Only a pending order can be approved, rejected or cancelled."),
    SAAS_010("The company already has a pending order."),
    SAAS_011("The plan user limit ({0}) is reached. Upgrade the plan to add accounts."),
    SAAS_012("A percentage discount must be between 0 and 100."),
    SAAS_013("The default plan cannot be deleted."),
    SAAS_014("The plan is not available.");

    private final String message;
}
