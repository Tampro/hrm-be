package vn.specom.hrm.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.exception.ECommonError;

import java.util.Optional;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Optional<HrmPrincipal> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof HrmPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static HrmPrincipal get() {
        return find().orElseThrow(() -> ApiException.unauthorized(ECommonError.COMMON_003));
    }
}
