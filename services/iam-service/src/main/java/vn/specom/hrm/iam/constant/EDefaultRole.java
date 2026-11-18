package vn.specom.hrm.iam.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Roles every new company starts with (docs/analysis/04 §3.3: WorkDo's company/hr/manager/employee plus SPECOM's
 * executive and accountant). Permission sets cover only the IAM module for now; each business module adds its
 * codes here when it is built.
 */
@Getter
@RequiredArgsConstructor
public enum EDefaultRole {
    COMPANY_ADMIN("Company administrator", union(IamPermissions.ALL, vn.specom.hrm.iam.saas.SaasPermissions.ALL)),
    // TODO(assumption): HR manages accounts but not roles — confirm with BA.
    HR("HR", Set.of(IamPermissions.USER_VIEW, IamPermissions.USER_CREATE, IamPermissions.USER_UPDATE,
            IamPermissions.ROLE_VIEW, IamPermissions.PERMISSION_VIEW)),
    MANAGER("Manager", Set.of()),
    EMPLOYEE("Employee", Set.of()),
    EXECUTIVE("Executive board", Set.of()),
    ACCOUNTANT("Accountant", Set.of());

    private static Set<String> union(Set<String> a, Set<String> b) {
        java.util.Set<String> all = new java.util.HashSet<>(a);
        all.addAll(b);
        return Set.copyOf(all);
    }

    private final String displayName;
    private final Set<String> permissions;
}
