package vn.specom.hrm.iam.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.specom.hrm.exception.ErrorCode;

/** Never renumber or reuse a code: the FE and the app key on it. */
@Getter
@RequiredArgsConstructor
public enum EIamError implements ErrorCode {
    AUTH_001("Username or password is incorrect."),
    AUTH_002("The account is locked or does not belong to an active company."),
    AUTH_003("The refresh token is invalid or expired."),
    AUTH_004("The password must be changed before the first login."),
    AUTH_005("The authentication server is unavailable. Please try again later."),
    AUTH_006("This account is signed in but has no HRM profile. Contact your administrator."),
    AUTH_007("The new password does not meet the password policy."),
    AUTH_008("Too many failed attempts. The account is temporarily locked."),
    AUTH_009("The company subscription has expired. Ask your company administrator to renew it."),
    IAM_001("The company does not exist."),
    IAM_002("The company code is already taken."),
    IAM_003("A company must be specified for this action."),
    IAM_004("The role does not exist."),
    IAM_005("The role code is already taken in this company."),
    IAM_006("A system role cannot be deleted or renamed."),
    IAM_007("The role is still assigned to users."),
    IAM_008("Unknown permission code(s): {0}."),
    IAM_009("The user does not exist."),
    IAM_010("The username is already taken."),
    IAM_011("One or more roles do not exist in this company."),
    IAM_012("The permissions of the company administrator role cannot be reduced."),
    IAM_013("You cannot change your own roles or status."),
    IAM_014("The user could not be created in the authentication server."),
    IAM_015("You cannot grant permissions you do not hold: {0}.");

    private final String message;
}
