package vn.specom.hrm.iam.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.auth.AuthForms.TokenDto;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.keycloak.KeycloakClient;
import vn.specom.hrm.iam.keycloak.KeycloakTokens;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.security.HrmJwtAuthenticationConverter;

import java.util.Optional;

/**
 * Login facade over Keycloak, so the FE keeps its {@code POST /auth/login {username, password}} contract
 * (docs/analysis/04 §4). Keycloak checks the password, brute-force lockout and sessions; this service adds the HRM
 * rules: the account must exist here, be ACTIVE, and belong to an ACTIVE company.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final KeycloakClient keycloak;
    private final JwtDecoder jwtDecoder;
    private final UserAccountRepository users;
    private final CompanyRepository companies;

    @Transactional(readOnly = true)
    public TokenDto login(String username, String password) {
        KeycloakTokens tokens = keycloak.passwordGrant(username, password);
        assertMayUseHrm(tokens);
        return toDto(tokens);
    }

    /** Keycloak rotates the refresh token (revokeRefreshToken in the realm), so the old one stops working. */
    @Transactional(readOnly = true)
    public TokenDto refresh(String refreshToken) {
        KeycloakTokens tokens = keycloak.refresh(refreshToken);
        assertMayUseHrm(tokens);
        return toDto(tokens);
    }

    public void logout(String refreshToken) {
        keycloak.logout(refreshToken);
    }

    /**
     * First login with an administrator-given temporary password. Keycloak refuses the password grant with
     * "not fully set up" only after the password matched, so that answer is the proof of the current password;
     * any other outcome is reported as is and nothing is changed.
     */
    @Transactional(readOnly = true)
    public TokenDto setInitialPassword(String username, String currentPassword, String newPassword) {
        try {
            KeycloakTokens tokens = keycloak.passwordGrant(username, currentPassword);
            // No pending action: the password is already permanent — just log in.
            assertMayUseHrm(tokens);
            return toDto(tokens);
        } catch (ApiException e) {
            if (e.getErrorCode() != EIamError.AUTH_004) {
                throw e;
            }
        }
        String keycloakId = keycloak.findUserId(username);
        if (keycloakId == null) {
            throw ApiException.unauthorized(EIamError.AUTH_001);
        }
        keycloak.setPermanentPassword(keycloakId, newPassword);
        return login(username, newPassword);
    }

    private void assertMayUseHrm(KeycloakTokens tokens) {
        Jwt jwt = jwtDecoder.decode(tokens.accessToken());
        if (HrmJwtAuthenticationConverter.isSuperAdmin(jwt)) {
            return;
        }
        Optional<UserAccount> account = users.findByKeycloakIdAndDeletedFalse(jwt.getSubject());
        if (account.isEmpty()) {
            keycloak.logout(tokens.refreshToken());
            throw ApiException.forbidden(EIamError.AUTH_006);
        }
        UserAccount u = account.get();
        boolean companyActive = companies.findByIdAndDeletedFalse(u.getCompanyId())
                .map(c -> c.getStatus() == ECompanyStatus.ACTIVE).orElse(false);
        if (u.getStatus() != EUserStatus.ACTIVE || !companyActive) {
            keycloak.logout(tokens.refreshToken());
            throw ApiException.unauthorized(EIamError.AUTH_002);
        }
    }

    private static TokenDto toDto(KeycloakTokens t) {
        return new TokenDto(t.accessToken(), t.refreshToken(), t.expiresIn(), t.refreshExpiresIn());
    }
}
