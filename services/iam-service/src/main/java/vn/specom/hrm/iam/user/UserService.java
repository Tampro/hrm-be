package vn.specom.hrm.iam.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.keycloak.KeycloakClient;
import vn.specom.hrm.iam.model.Permission;
import vn.specom.hrm.iam.model.Role;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.RoleRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;
import vn.specom.hrm.iam.user.UserDtos.CreatedUserDto;
import vn.specom.hrm.iam.user.UserDtos.UserCreateForm;
import vn.specom.hrm.iam.user.UserDtos.UserDto;
import vn.specom.hrm.security.CurrentUser;
import vn.specom.hrm.security.HrmPrincipal;
import vn.specom.hrm.security.TenantGuard;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final KeycloakClient keycloak;
    private final LocalAuthorityResolver authorityCache;
    private final vn.specom.hrm.iam.repository.CompanyRepository companies;
    private final vn.specom.hrm.iam.saas.SubscriptionService subscriptions;
    private final vn.specom.hrm.iam.email.EmailNotifier notifier;

    @Transactional(readOnly = true)
    public Page<UserDto> search(Long requestedCompanyId, String keyword, Pageable pageable) {
        Long companyId = TenantGuard.resolveCompany(requestedCompanyId, EIamError.IAM_003);
        return users.search(companyId, keyword == null ? "" : keyword.trim(), pageable).map(UserDto::of);
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return UserDto.of(require(id));
    }

    /**
     * Creates the Keycloak user first (with a temporary password), then the HRM row. If the database write fails
     * after Keycloak succeeded, the Keycloak user is left without an HRM profile: it cannot use HRM (AUTH_006) and
     * an administrator can retry with the same username once it is removed in Keycloak.
     * TODO(assumption): no compensation (delete in Keycloak) yet — decide whether orphans need cleanup.
     */
    @Transactional
    public CreatedUserDto create(Long requestedCompanyId, UserCreateForm form) {
        Long companyId = TenantGuard.resolveCompany(requestedCompanyId, EIamError.IAM_003);
        return createInCompany(companyId, form);
    }

    @Transactional
    public CreatedUserDto createInCompany(Long companyId, UserCreateForm form) {
        return createInCompany(companyId, form, false);
    }

    /** @param firstAdminOfNewCompany sends COMPANY_CREATE instead of USER_CREATE */
    @Transactional
    public CreatedUserDto createInCompany(Long companyId, UserCreateForm form, boolean firstAdminOfNewCompany) {
        if (users.existsByUsernameAndDeletedFalse(form.username())) {
            throw ApiException.badRequest(EIamError.IAM_010);
        }
        var company = companies.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> ApiException.notFound(EIamError.IAM_001));
        subscriptions.assertCanAddUser(company);
        Set<Long> roleIds = new HashSet<>(form.roleIds());
        if (form.roleCodes() != null && !form.roleCodes().isEmpty()) {
            List<Role> byCode = roles.findByCompanyIdAndCodeInAndDeletedFalse(companyId, form.roleCodes());
            if (byCode.size() != form.roleCodes().size()) {
                throw ApiException.badRequest(EIamError.IAM_011);
            }
            byCode.forEach(r -> roleIds.add(r.getId()));
        }
        Set<Role> assigned = resolveAssignable(companyId, roleIds);
        String temporaryPassword = form.password() != null ? form.password() : generatePassword();
        String keycloakId = keycloak.createUser(form.username(), form.email(), form.fullName(), temporaryPassword);

        UserAccount u = new UserAccount();
        u.setKeycloakId(keycloakId);
        u.setCompanyId(companyId);
        u.setUsername(form.username());
        u.setEmail(form.email());
        u.setFullName(form.fullName());
        u.setStatus(EUserStatus.ACTIVE);
        u.setRoles(assigned);
        UserAccount saved = users.save(u);
        // Sent after commit; the temporary password only travels in this email and the response.
        if (firstAdminOfNewCompany) {
            notifier.companyCreated(saved, company, temporaryPassword);
        } else {
            notifier.userCreated(saved, company, temporaryPassword);
        }
        return new CreatedUserDto(UserDto.of(saved), temporaryPassword);
    }

    @Transactional
    public UserDto replaceRoles(Long id, Set<Long> roleIds) {
        UserAccount u = require(id);
        assertNotSelf(u);
        u.setRoles(resolveAssignable(u.getCompanyId(), roleIds));
        authorityCache.evictAll();
        return UserDto.of(u);
    }

    /** LOCKED disables the Keycloak user and ends its sessions; the 30 s authority cache is cleared too. */
    @Transactional
    public UserDto changeStatus(Long id, EUserStatus status) {
        UserAccount u = require(id);
        assertNotSelf(u);
        u.setStatus(status);
        keycloak.setEnabled(u.getKeycloakId(), status == EUserStatus.ACTIVE);
        authorityCache.evictAll();
        return UserDto.of(u);
    }

    /** Administrator sets a permanent password for an account of their company ("Change Password"). */
    @Transactional
    public UserDto changePassword(Long id, String newPassword) {
        UserAccount u = require(id);
        assertNotSelf(u);
        keycloak.setPermanentPassword(u.getKeycloakId(), newPassword);
        return UserDto.of(u);
    }

    private UserAccount require(Long id) {
        UserAccount u = users.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(EIamError.IAM_009));
        TenantGuard.assertSameCompany(u.getCompanyId(), EIamError.IAM_009);
        return u;
    }

    private static void assertNotSelf(UserAccount target) {
        if (Objects.equals(CurrentUser.get().userId(), target.getId())) {
            throw ApiException.badRequest(EIamError.IAM_013);
        }
    }

    /**
     * Roles must be of the target company, and a company user may only hand out roles whose permissions they hold
     * themselves — otherwise IAM_USER_UPDATE alone would let HR make anyone COMPANY_ADMIN.
     */
    private Set<Role> resolveAssignable(Long companyId, Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> found = roles.findByCompanyIdAndIdInAndDeletedFalse(companyId, roleIds);
        if (found.size() != roleIds.size()) {
            throw ApiException.badRequest(EIamError.IAM_011);
        }
        HrmPrincipal caller = CurrentUser.get();
        if (!caller.superAdmin()) {
            Set<String> needed = found.stream().flatMap(r -> r.getPermissions().stream())
                    .map(Permission::getCode).collect(Collectors.toSet());
            needed.removeAll(caller.permissions());
            if (!needed.isEmpty()) {
                throw ApiException.forbidden(EIamError.IAM_015, String.join(", ", needed));
            }
        }
        return new HashSet<>(found);
    }

    /** 16 chars with at least one of each class, so it always meets the realm policy (deploy/keycloak). */
    static String generatePassword() {
        String[] classes = {"ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%"};
        List<Character> chars = new java.util.ArrayList<>(16);
        for (String c : classes) {
            chars.add(c.charAt(RANDOM.nextInt(c.length())));
        }
        while (chars.size() < 16) {
            chars.add(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        java.util.Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder(16);
        chars.forEach(sb::append);
        return sb.toString();
    }
}
