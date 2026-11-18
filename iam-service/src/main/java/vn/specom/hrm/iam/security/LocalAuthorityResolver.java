package vn.specom.hrm.iam.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.model.Permission;
import vn.specom.hrm.iam.model.Role;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.security.AuthorityResolver;
import vn.specom.hrm.security.HrmJwtAuthenticationConverter;
import vn.specom.hrm.security.HrmPrincipal;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the caller from the IAM database on every request (behind a 30 s cache). A valid Keycloak token is not
 * enough: a locked account or a suspended company is rejected with 401 here, even before the token expires.
 */
@Component
@RequiredArgsConstructor
public class LocalAuthorityResolver implements AuthorityResolver {
    private final UserAccountRepository users;
    private final CompanyRepository companies;

    private final Cache<String, Optional<HrmPrincipal>> cache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).maximumSize(10_000).build();

    @Override
    public HrmPrincipal resolve(Jwt jwt) {
        boolean superAdmin = HrmJwtAuthenticationConverter.isSuperAdmin(jwt);
        Optional<HrmPrincipal> local = cache.get(jwt.getSubject(), this::load);
        if (local.isPresent()) {
            HrmPrincipal p = local.get();
            return superAdmin ? new HrmPrincipal(p.userId(), p.keycloakId(), p.username(), p.companyId(), true,
                    p.roles(), p.permissions()) : p;
        }
        if (superAdmin) {
            return new HrmPrincipal(null, jwt.getSubject(), jwt.getClaimAsString("preferred_username"), null, true,
                    Set.of(), Set.of());
        }
        // Signed in at Keycloak but no HRM profile: authenticated with no permissions, so GET /me can explain it.
        return new HrmPrincipal(null, jwt.getSubject(), jwt.getClaimAsString("preferred_username"), null, false,
                Set.of(), Set.of());
    }

    /** Called after any change to roles, permissions, user roles or status. */
    public void evictAll() {
        cache.invalidateAll();
    }

    // No transaction needed: the entity graph fetches roles and permissions in the one query.
    private Optional<HrmPrincipal> load(String keycloakId) {
        Optional<UserAccount> found = users.findByKeycloakIdAndDeletedFalse(keycloakId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UserAccount u = found.get();
        boolean companyActive = companies.findByIdAndDeletedFalse(u.getCompanyId())
                .map(c -> c.getStatus() == ECompanyStatus.ACTIVE).orElse(false);
        if (u.getStatus() != EUserStatus.ACTIVE || !companyActive) {
            throw new InvalidBearerTokenException("Account locked or company inactive");
        }
        Set<Role> roles = u.getRoles().stream().filter(r -> !r.isDeleted()).collect(Collectors.toSet());
        return Optional.of(new HrmPrincipal(
                u.getId(), u.getKeycloakId(), u.getUsername(), u.getCompanyId(), false,
                roles.stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet()),
                roles.stream().flatMap(r -> r.getPermissions().stream()).map(Permission::getCode)
                        .collect(Collectors.toUnmodifiableSet())));
    }
}
