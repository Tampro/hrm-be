package vn.specom.hrm.iam.company;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreateForm;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyCreatedDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyInfoDto;
import vn.specom.hrm.iam.company.CompanyDtos.CompanyUpdateForm;
import vn.specom.hrm.iam.company.CompanyDtos.StatusCountsDto;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EDefaultRole;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.keycloak.KeycloakClient;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.model.Plan;
import vn.specom.hrm.iam.model.Role;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.PlanRepository;
import vn.specom.hrm.iam.repository.RoleRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.iam.role.RoleService;
import vn.specom.hrm.iam.saas.EBillingCycle;
import vn.specom.hrm.iam.saas.OrderService;
import vn.specom.hrm.iam.saas.SaasDtos.OrderDto;
import vn.specom.hrm.iam.saas.SubscriptionService;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;
import vn.specom.hrm.iam.user.UserDtos.CreatedUserDto;
import vn.specom.hrm.iam.user.UserDtos.UserCreateForm;
import vn.specom.hrm.iam.user.UserDtos.UserDto;
import vn.specom.hrm.iam.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tenant lifecycle and the super admin companies list (docs/analysis/06 §3.2.1). Super admin only. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {
    private static final Instant MIN = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant MAX = Instant.parse("9999-01-01T00:00:00Z");

    private final CompanyRepository companies;
    private final RoleRepository roles;
    private final PlanRepository plans;
    private final UserAccountRepository users;
    private final RoleService roleService;
    private final UserService userService;
    private final OrderService orderService;
    private final SubscriptionService subscriptions;
    private final KeycloakClient keycloak;
    private final LocalAuthorityResolver authorityCache;
    private final vn.specom.hrm.iam.referral.ReferralService referral;

    /**
     * @param from first created day included (UTC); null = no lower bound
     * @param to   last created day included (UTC); null = no upper bound
     */
    @Transactional(readOnly = true)
    public Page<CompanyDto> search(String keyword, LocalDate from, LocalDate to, ECompanyStatus status,
                                   Pageable pageable) {
        Map<Long, Plan> live = livePlans();
        return companies.search(kw(keyword), start(from), end(to), status, pageable)
                .map(c -> CompanyDto.of(c, subscriptions.effective(c, live)));
    }

    @Transactional(readOnly = true)
    public StatusCountsDto countByStatus(String keyword, LocalDate from, LocalDate to) {
        Map<ECompanyStatus, Long> counts = companies.countByStatus(kw(keyword), start(from), end(to)).stream()
                .collect(Collectors.toMap(r -> (ECompanyStatus) r[0], r -> (Long) r[1]));
        long active = counts.getOrDefault(ECompanyStatus.ACTIVE, 0L);
        long inactive = counts.getOrDefault(ECompanyStatus.SUSPENDED, 0L);
        return new StatusCountsDto(active + inactive, active, inactive);
    }

    @Transactional(readOnly = true)
    public CompanyInfoDto info(Long id) {
        Company c = require(id);
        var e = subscriptions.effective(c);
        var admins = users.findByCompanyAndRole(c.getId(), EDefaultRole.COMPANY_ADMIN.name()).stream()
                .map(UserDto::of).toList();
        return new CompanyInfoDto(CompanyDto.of(c, e), users.countByCompanyIdAndDeletedFalse(c.getId()),
                e.plan() == null ? null : e.plan().getMaxUsers(), admins);
    }

    @Transactional(readOnly = true)
    public CompanyDto get(Long id) {
        Company c = require(id);
        return CompanyDto.of(c, subscriptions.effective(c));
    }

    /** Company + default roles + default plan + first administrator (the Keycloak user is created last). */
    @Transactional
    public CompanyCreatedDto create(CompanyCreateForm form) {
        if (companies.existsByCodeAndDeletedFalse(form.code())) {
            throw ApiException.badRequest(EIamError.IAM_002);
        }
        Company c = new Company();
        c.setCode(form.code());
        c.setName(form.name());
        c.setEmail(form.email());
        c.setStatus(Boolean.FALSE.equals(form.enableLogin()) ? ECompanyStatus.SUSPENDED : ECompanyStatus.ACTIVE);
        subscriptions.assignDefault(c);
        c.setReferredByCompanyId(referral.referrerOf(form.referralCode()));
        c = companies.save(c);
        c.setReferralCode(vn.specom.hrm.iam.referral.ReferralService.codeFor(c.getId()));
        roleService.createDefaultRoles(c.getId());
        Role admin = roles.findByCompanyIdAndCodeAndDeletedFalse(c.getId(), EDefaultRole.COMPANY_ADMIN.name())
                .orElseThrow();
        CreatedUserDto adminUser = userService.createInCompany(c.getId(), new UserCreateForm(
                form.admin().username(), form.admin().email(), form.admin().fullName(), Set.of(admin.getId())), true);
        return new CompanyCreatedDto(CompanyDto.of(c, subscriptions.effective(c)), adminUser);
    }

    /** Full replacement of the editable profile (the code is the tenant key and stays). */
    @Transactional
    public CompanyDto update(Long id, CompanyUpdateForm form) {
        Company c = require(id);
        c.setName(form.name());
        c.setEmail(form.email());
        return CompanyDto.of(c, subscriptions.effective(c));
    }

    /** "Disable Login": SUSPENDED rejects logins and every token of the company within the 30 s authority cache. */
    @Transactional
    public CompanyDto changeStatus(Long id, ECompanyStatus status) {
        Company c = require(id);
        c.setStatus(status);
        authorityCache.evictAll();
        return CompanyDto.of(c, subscriptions.effective(c));
    }

    @Transactional
    public OrderDto upgradePlan(Long id, Long planId, EBillingCycle cycle) {
        require(id);
        return orderService.upgradeByAdmin(id, planId, cycle);
    }

    /**
     * "Reset Password" (PO, 2026-11-18: the super admin types the new password): sets it as permanent for an account
     * of the company — by default its first active COMPANY_ADMIN — and ends every session of that account. The
     * password is neither stored nor logged.
     */
    @Transactional(readOnly = true)
    public UserDto resetPassword(Long id, Long userId, String newPassword) {
        Company c = require(id);
        UserAccount target = userId != null
                ? users.findByIdAndDeletedFalse(userId).filter(u -> u.getCompanyId().equals(c.getId()))
                        .orElseThrow(() -> ApiException.notFound(EIamError.IAM_009))
                : users.findByCompanyAndRole(c.getId(), EDefaultRole.COMPANY_ADMIN.name()).stream()
                        .filter(u -> u.getStatus() == EUserStatus.ACTIVE)
                        .findFirst().orElseThrow(() -> ApiException.badRequest(EIamError.IAM_016));
        keycloak.setPermanentPassword(target.getKeycloakId(), newPassword);
        keycloak.logoutSessions(target.getKeycloakId());
        log.info("Super admin reset the password of user {} in company {}", target.getId(), c.getCode());
        return UserDto.of(target);
    }

    /**
     * Soft delete (PO, 2026-11-18): rows stay for history, every account is disabled in Keycloak (sessions end), open
     * orders are cancelled, and the code becomes free for a new company. Not reversible through the API.
     */
    @Transactional
    public void delete(Long id) {
        Company c = require(id);
        for (UserAccount u : users.findByCompanyIdAndDeletedFalse(c.getId())) {
            keycloak.setEnabled(u.getKeycloakId(), false);
        }
        orderService.cancelPendingOf(c.getId());
        c.setDeleted(true);
        c.setStatus(ECompanyStatus.SUSPENDED);
        authorityCache.evictAll();
        log.info("Company {} deleted (soft)", c.getCode());
    }

    private Company require(Long id) {
        return companies.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(EIamError.IAM_001));
    }

    private Map<Long, Plan> livePlans() {
        return plans.findByDeletedFalseOrderByMonthlyPriceAsc().stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));
    }

    private static String kw(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private static Instant start(LocalDate from) {
        return from == null ? MIN : from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant end(LocalDate to) {
        return to == null ? MAX : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
