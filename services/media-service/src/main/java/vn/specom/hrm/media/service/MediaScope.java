package vn.specom.hrm.media.service;

import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.exception.ECommonError;
import vn.specom.hrm.exception.ErrorCode;
import vn.specom.hrm.security.CurrentUser;
import vn.specom.hrm.security.HrmPrincipal;

import java.util.Objects;

/**
 * Whose files a request touches (docs/analysis/06 §3.2.2 note: the super admin sees the files of every company, a
 * company only its own). {@code companyId} null with {@code all} false = the platform's own files.
 */
record MediaScope(boolean all, Long companyId) {

    /** Lists and stats. Super admin: a named company, or everything when none is named. */
    static MediaScope forRead(Long requestedCompanyId) {
        HrmPrincipal p = CurrentUser.get();
        if (p.superAdmin()) {
            return new MediaScope(requestedCompanyId == null, requestedCompanyId);
        }
        return new MediaScope(false, ownCompany(p));
    }

    /** Uploads and folders. Super admin: a named company, or the platform when none is named. */
    static MediaScope forWrite(Long requestedCompanyId) {
        HrmPrincipal p = CurrentUser.get();
        return p.superAdmin() ? new MediaScope(false, requestedCompanyId) : new MediaScope(false, ownCompany(p));
    }

    /** By-id access: another company's row answers 404, so its existence is not confirmed. */
    static void assertOwns(Long rowCompanyId, ErrorCode notFound) {
        HrmPrincipal p = CurrentUser.get();
        if (!p.superAdmin() && !Objects.equals(ownCompany(p), rowCompanyId)) {
            throw ApiException.notFound(notFound);
        }
    }

    String storageOwner() {
        return companyId == null ? "platform" : "c" + companyId;
    }

    private static Long ownCompany(HrmPrincipal p) {
        if (p.companyId() == null) {
            throw ApiException.forbidden(ECommonError.COMMON_004);
        }
        return p.companyId();
    }
}
