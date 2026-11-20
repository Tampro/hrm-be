package vn.specom.hrm.asset.common;

import vn.specom.hrm.security.TenantGuard;

import java.util.Locale;

/** Small helpers shared by the asset pages. */
public final class Texts {
    public static final String COMPANY = "Super admin only: the company to act on. Ignored for company users.";

    private Texts() {
    }

    public static Long company(Long requested) {
        return TenantGuard.resolveCompany(requested, EAssetError.AST_001);
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** LIKE pattern for a keyword, '' when none; ! escapes % and _. */
    public static String kw(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        String k = keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + k + "%";
    }
}
