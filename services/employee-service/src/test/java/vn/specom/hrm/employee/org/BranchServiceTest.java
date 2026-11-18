package vn.specom.hrm.employee.org;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.specom.hrm.employee.TestLogin;
import vn.specom.hrm.employee.common.EEmployeeError;
import vn.specom.hrm.employee.document.EmployeeDocumentRepository;
import vn.specom.hrm.employee.employee.EmployeeRepository;
import vn.specom.hrm.employee.org.BranchDtos.BranchForm;
import vn.specom.hrm.exception.ApiException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BranchServiceTest {
    private final BranchRepository branches = mock(BranchRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final BranchService service = new BranchService(branches, departments, employees);
    private final OrgService orgs = new OrgService(branches, departments, mock(DesignationRepository.class),
            mock(DocumentTypeRepository.class), employees, mock(EmployeeDocumentRepository.class));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDefaultsToActiveAndTrimsFields() {
        TestLogin.as(10L, "ORG_MANAGE");
        when(branches.save(any())).thenAnswer(i -> {
            Branch b = i.getArgument(0);
            b.setId(1L);
            return b;
        });
        var d = service.create(null, new BranchForm(" HQ ", "  ", "Ha Noi", null, null, null, "024", "hq@example.com", null));
        assertThat(d.name()).isEqualTo("HQ");
        assertThat(d.address()).isNull();
        assertThat(d.status()).isEqualTo(EBranchStatus.ACTIVE);
        assertThat(d.companyId()).isEqualTo("10");
    }

    @Test
    void keywordIsEscapedAndLowerCased() {
        TestLogin.as(10L, "ORG_VIEW");
        when(branches.search(eq(10L), eq(null), any(), any())).thenReturn(Page.empty());
        service.search(null, " 50%_A ", null, PageRequest.of(0, 20));
        verify(branches).search(10L, null, "%50\\%\\_a%", PageRequest.of(0, 20));
    }

    @Test
    void branchInUseCannotBeDeleted() {
        TestLogin.as(10L, "ORG_MANAGE");
        when(branches.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(branch(1L, 10L, EBranchStatus.ACTIVE)));
        when(employees.existsByBranchIdAndDeletedFalse(1L)).thenReturn(true);
        assertError(() -> service.delete(1L), EEmployeeError.EMP_010);
    }

    @Test
    void branchOfAnotherCompanyIsNotFound() {
        TestLogin.as(10L, "ORG_VIEW");
        when(branches.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(branch(1L, 11L, EBranchStatus.ACTIVE)));
        assertError(() -> service.get(1L), EEmployeeError.EMP_002);
    }

    @Test
    void inactiveBranchCannotBeChosenButCanBeKept() {
        TestLogin.as(10L, "EMPLOYEE_UPDATE");
        when(branches.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(branch(1L, 10L, EBranchStatus.INACTIVE)));
        assertError(() -> orgs.assertChain(10L, 1L, null, null, null), EEmployeeError.EMP_029);
        orgs.assertChain(10L, 1L, null, null, 1L);
        assertError(() -> orgs.createDepartment(1L, "IT"), EEmployeeError.EMP_029);
    }

    @Test
    void optionsListOnlyActiveBranches() {
        TestLogin.as(10L, "EMPLOYEE_VIEW");
        when(branches.findByCompanyIdAndStatusAndDeletedFalseOrderByNameAsc(10L, EBranchStatus.ACTIVE))
                .thenReturn(List.of(branch(2L, 10L, EBranchStatus.ACTIVE)));
        assertThat(service.options(null)).extracting(BranchDtos.BranchOption::id).containsExactly("2");
    }

    private static Branch branch(Long id, Long companyId, EBranchStatus status) {
        Branch b = new Branch();
        b.setId(id);
        b.setCompanyId(companyId);
        b.setName("B" + id);
        b.setStatus(status);
        return b;
    }

    private static void assertError(Runnable call, EEmployeeError code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(code);
    }
}
