package vn.specom.hrm.iam.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.constant.EIamError;
import vn.specom.hrm.iam.constant.EUserStatus;
import vn.specom.hrm.iam.keycloak.KeycloakClient;
import vn.specom.hrm.iam.model.Company;
import vn.specom.hrm.iam.model.UserAccount;
import vn.specom.hrm.iam.repository.CompanyRepository;
import vn.specom.hrm.iam.repository.PlanRepository;
import vn.specom.hrm.iam.repository.RoleRepository;
import vn.specom.hrm.iam.repository.UserAccountRepository;
import vn.specom.hrm.iam.role.RoleService;
import vn.specom.hrm.iam.saas.OrderService;
import vn.specom.hrm.iam.saas.SubscriptionService;
import vn.specom.hrm.iam.security.LocalAuthorityResolver;
import vn.specom.hrm.iam.user.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceTest {
    private CompanyRepository companies;
    private UserAccountRepository users;
    private KeycloakClient keycloak;
    private OrderService orders;
    private CompanyService service;
    private Company company;

    @BeforeEach
    void setUp() {
        companies = mock(CompanyRepository.class);
        users = mock(UserAccountRepository.class);
        keycloak = mock(KeycloakClient.class);
        orders = mock(OrderService.class);
        service = new CompanyService(companies, mock(RoleRepository.class), mock(PlanRepository.class), users,
                mock(RoleService.class), mock(UserService.class), orders, mock(SubscriptionService.class), keycloak,
                mock(LocalAuthorityResolver.class));
        company = new Company();
        company.setId(10L);
        company.setCode("ACME");
        company.setStatus(ECompanyStatus.ACTIVE);
        when(companies.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(company));
    }

    @Test
    void resetPasswordDefaultsToTheFirstActiveCompanyAdminAndEndsItsSessions() {
        UserAccount locked = user(1L, 10L, "kc-1", EUserStatus.LOCKED);
        UserAccount active = user(2L, 10L, "kc-2", EUserStatus.ACTIVE);
        when(users.findByCompanyAndRole(10L, "COMPANY_ADMIN")).thenReturn(List.of(locked, active));

        assertThat(service.resetPassword(10L, null, "N3w-Passw0rd!").id()).isEqualTo("2");
        verify(keycloak).setPermanentPassword("kc-2", "N3w-Passw0rd!");
        verify(keycloak).logoutSessions("kc-2");
    }

    @Test
    void resetPasswordRefusesAUserOfAnotherCompany() {
        when(users.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(user(5L, 99L, "kc-5", EUserStatus.ACTIVE)));

        assertThatThrownBy(() -> service.resetPassword(10L, 5L, "N3w-Passw0rd!"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(EIamError.IAM_009);
        verify(keycloak, never()).setPermanentPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordWithoutActiveAdminIsReported() {
        when(users.findByCompanyAndRole(10L, "COMPANY_ADMIN")).thenReturn(List.of());
        assertThatThrownBy(() -> service.resetPassword(10L, null, "N3w-Passw0rd!"))
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(EIamError.IAM_016);
    }

    @Test
    void deleteIsSoftDisablesEveryAccountAndCancelsOpenOrders() {
        when(users.findByCompanyIdAndDeletedFalse(10L)).thenReturn(List.of(
                user(1L, 10L, "kc-1", EUserStatus.ACTIVE), user(2L, 10L, "kc-2", EUserStatus.LOCKED)));

        service.delete(10L);

        assertThat(company.isDeleted()).isTrue();
        assertThat(company.getStatus()).isEqualTo(ECompanyStatus.SUSPENDED);
        verify(keycloak).setEnabled("kc-1", false);
        verify(keycloak).setEnabled("kc-2", false);
        verify(orders).cancelPendingOf(10L);
    }

    private static UserAccount user(Long id, Long companyId, String kc, EUserStatus status) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setCompanyId(companyId);
        u.setKeycloakId(kc);
        u.setUsername("user" + id);
        u.setStatus(status);
        return u;
    }
}
