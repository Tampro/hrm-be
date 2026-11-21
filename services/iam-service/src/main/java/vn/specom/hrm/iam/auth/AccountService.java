package vn.specom.hrm.iam.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.company.CompanyService;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.email.EmailNotifier;
import vn.specom.hrm.iam.keycloak.KeycloakClient;
import vn.specom.hrm.iam.model.AccountToken;
import vn.specom.hrm.iam.model.AccountToken.EPurpose;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.AccountTokenRepository;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.iam.settings.SettingsService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * The public account flows of docs/analysis/06 §2.4–2.6: Forgot password (one-time link by email), Reset password
 * and Sign up. The answers never tell whether an email has an account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    static final Duration RESET_VALIDITY = Duration.ofMinutes(30);

    private final UserAccountRepository users;
    private final CompanyRepository companies;
    private final AccountTokenRepository tokens;
    private final KeycloakClient keycloak;
    private final EmailNotifier notifier;
    private final SettingsService settings;
    private final CompanyService companyService;
    private final Clock clock;

    @Value("${hrm.app-url:http://localhost:5173}")
    private String appUrl;

    /**
     * Sends a reset link to every active account with this email in an active company. Super admins are not HRM
     * accounts (Keycloak only) and get nothing. TODO(assumption): an operator resets a super admin in Keycloak.
     */
    @Transactional
    public void forgotPassword(String email) {
        Instant now = clock.instant();
        for (UserAccount u : users.findByEmailIgnoreCaseAndDeletedFalse(email.trim())) {
            Company c = companies.findByIdAndDeletedFalse(u.getCompanyId()).orElse(null);
            if (u.getStatus() != EUserStatus.ACTIVE || c == null || c.getStatus() != ECompanyStatus.ACTIVE) {
                continue;
            }
            tokens.findByPurposeAndUserIdAndUsedAtIsNull(EPurpose.PASSWORD_RESET, u.getId()).forEach(t -> t.setUsedAt(now));
            String token = AccountTokens.newToken();
            AccountToken t = new AccountToken();
            t.setPurpose(EPurpose.PASSWORD_RESET);
            t.setUserId(u.getId());
            t.setCompanyId(u.getCompanyId());
            t.setTokenHash(AccountTokens.hash(token));
            t.setExpiresAt(now.plus(RESET_VALIDITY));
            tokens.save(t);
            notifier.passwordReset(u, c, appUrl + "/reset-password?token=" + token, RESET_VALIDITY.toMinutes());
        }
    }

    /** Sets the new password, spends the link and ends every session of the account. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = clock.instant();
        AccountToken t = tokens.findByTokenHash(AccountTokens.hash(token))
                .filter(x -> x.getPurpose() == EPurpose.PASSWORD_RESET && x.getUsedAt() == null
                        && x.getExpiresAt().isAfter(now))
                .orElseThrow(() -> ApiException.badRequest(EIamError.AUTH_014));
        UserAccount u = users.findByIdAndDeletedFalse(t.getUserId())
                .filter(x -> x.getStatus() == EUserStatus.ACTIVE)
                .orElseThrow(() -> ApiException.badRequest(EIamError.AUTH_014));
        keycloak.setPermanentPassword(u.getKeycloakId(), newPassword);
        keycloak.logoutSessions(u.getKeycloakId());
        t.setUsedAt(now);
        log.info("Password of user {} reset by email link", u.getId());
    }

    /** Sign up: only with System Settings → User Registration on (AUTH_015). */
    @Transactional
    public Company register(String fullName, String email, String password, String companyName, String referralCode) {
        if (!settings.system().userRegistration()) {
            throw ApiException.forbidden(EIamError.AUTH_015);
        }
        String mail = email.trim().toLowerCase(Locale.ROOT);
        String name = companyName == null || companyName.isBlank() ? fullName.trim() : companyName.trim();
        return companyService.register(name, mail, fullName.trim(), password, referralCode);
    }
}
