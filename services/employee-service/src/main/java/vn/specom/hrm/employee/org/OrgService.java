package vn.specom.hrm.employee.org;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.specom.hrm.employee.common.EEmployeeError;
import vn.specom.hrm.employee.document.EmployeeDocumentRepository;
import vn.specom.hrm.employee.employee.EmployeeRepository;
import vn.specom.hrm.employee.org.OrgDtos.DepartmentDto;
import vn.specom.hrm.employee.org.OrgDtos.DesignationDto;
import vn.specom.hrm.employee.org.OrgDtos.DocumentTypeDto;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.security.TenantGuard;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Branch → department → designation, each owned by a company. A child must belong to its parent; a row still used
 * by a child or an employee cannot be deleted (EMP_010).
 */
@Service
@RequiredArgsConstructor
public class OrgService {
    private final BranchRepository branches;
    private final DepartmentRepository departments;
    private final DesignationRepository designations;
    private final DocumentTypeRepository documentTypes;
    private final EmployeeRepository employees;
    private final EmployeeDocumentRepository documents;

    // ------------------------------------------------------------------ departments

    @Transactional(readOnly = true)
    public List<DepartmentDto> departments(Long requestedCompanyId, Long branchId) {
        Long companyId = company(requestedCompanyId);
        List<Department> list = branchId == null
                ? departments.findByCompanyIdAndDeletedFalseOrderByNameAsc(companyId)
                : departments.findByCompanyIdAndBranchIdAndDeletedFalseOrderByNameAsc(companyId, branchId);
        return list.stream().map(DepartmentDto::of).toList();
    }

    @Transactional
    public DepartmentDto createDepartment(Long branchId, String name) {
        Branch b = requireBranch(branchId);
        if (b.getStatus() == EBranchStatus.INACTIVE) {
            throw ApiException.badRequest(EEmployeeError.EMP_029);
        }
        String n = name.trim();
        if (departments.existsByBranchIdAndNameAndDeletedFalseAndIdNot(branchId, n, -1L)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        Department d = new Department();
        d.setCompanyId(b.getCompanyId());
        d.setBranchId(branchId);
        d.setName(n);
        return DepartmentDto.of(departments.save(d));
    }

    /** Full replacement: moving to another branch is allowed while no designation or employee uses it. */
    @Transactional
    public DepartmentDto updateDepartment(Long id, Long branchId, String name) {
        Department d = requireDepartment(id);
        Branch b = requireBranch(branchId);
        if (!Objects.equals(b.getCompanyId(), d.getCompanyId())) {
            throw ApiException.notFound(EEmployeeError.EMP_002);
        }
        if (!Objects.equals(d.getBranchId(), branchId)
                && (designations.existsByDepartmentIdAndDeletedFalse(id) || employees.existsByDepartmentIdAndDeletedFalse(id))) {
            throw ApiException.badRequest(EEmployeeError.EMP_010);
        }
        String n = name.trim();
        if (departments.existsByBranchIdAndNameAndDeletedFalseAndIdNot(branchId, n, id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        d.setBranchId(branchId);
        d.setName(n);
        return DepartmentDto.of(d);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department d = requireDepartment(id);
        if (designations.existsByDepartmentIdAndDeletedFalse(id) || employees.existsByDepartmentIdAndDeletedFalse(id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_010);
        }
        d.setDeleted(true);
    }

    // ------------------------------------------------------------------ designations

    @Transactional(readOnly = true)
    public List<DesignationDto> designations(Long requestedCompanyId, Long departmentId) {
        Long companyId = company(requestedCompanyId);
        List<Designation> list = departmentId == null
                ? designations.findByCompanyIdAndDeletedFalseOrderByNameAsc(companyId)
                : designations.findByCompanyIdAndDepartmentIdAndDeletedFalseOrderByNameAsc(companyId, departmentId);
        return list.stream().map(DesignationDto::of).toList();
    }

    @Transactional
    public DesignationDto createDesignation(Long departmentId, String name) {
        Department dep = requireDepartment(departmentId);
        String n = name.trim();
        if (designations.existsByDepartmentIdAndNameAndDeletedFalseAndIdNot(departmentId, n, -1L)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        Designation d = new Designation();
        d.setCompanyId(dep.getCompanyId());
        d.setDepartmentId(departmentId);
        d.setName(n);
        return DesignationDto.of(designations.save(d));
    }

    @Transactional
    public DesignationDto updateDesignation(Long id, Long departmentId, String name) {
        Designation d = requireDesignation(id);
        Department dep = requireDepartment(departmentId);
        if (!Objects.equals(dep.getCompanyId(), d.getCompanyId())) {
            throw ApiException.notFound(EEmployeeError.EMP_003);
        }
        if (!Objects.equals(d.getDepartmentId(), departmentId) && employees.existsByDesignationIdAndDeletedFalse(id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_010);
        }
        String n = name.trim();
        if (designations.existsByDepartmentIdAndNameAndDeletedFalseAndIdNot(departmentId, n, id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        d.setDepartmentId(departmentId);
        d.setName(n);
        return DesignationDto.of(d);
    }

    @Transactional
    public void deleteDesignation(Long id) {
        Designation d = requireDesignation(id);
        if (employees.existsByDesignationIdAndDeletedFalse(id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_010);
        }
        d.setDeleted(true);
    }

    // ------------------------------------------------------------------ document types

    @Transactional(readOnly = true)
    public List<DocumentTypeDto> documentTypes(Long requestedCompanyId) {
        return documentTypes.visibleTo(company(requestedCompanyId)).stream().map(DocumentTypeDto::of).toList();
    }

    @Transactional
    public DocumentTypeDto createDocumentType(Long requestedCompanyId, String name) {
        Long companyId = company(requestedCompanyId);
        String n = name.trim();
        if (documentTypes.nameTaken(companyId, n, -1L)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        DocumentType t = new DocumentType();
        t.setCompanyId(companyId);
        t.setName(n);
        return DocumentTypeDto.of(documentTypes.save(t));
    }

    @Transactional
    public DocumentTypeDto renameDocumentType(Long id, String name) {
        DocumentType t = requireOwnDocumentType(id);
        String n = name.trim();
        if (documentTypes.nameTaken(t.getCompanyId(), n, id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_011);
        }
        t.setName(n);
        return DocumentTypeDto.of(t);
    }

    @Transactional
    public void deleteDocumentType(Long id) {
        DocumentType t = requireOwnDocumentType(id);
        if (documents.existsByDocumentTypeId(id)) {
            throw ApiException.badRequest(EEmployeeError.EMP_010);
        }
        t.setDeleted(true);
    }

    // ------------------------------------------------------------------ used by the employee module

    /**
     * Checks that branch ⊃ department ⊃ designation hold and belong to the company. Each level is optional, but a
     * lower level needs the one above it (EMP_023).
     */
    @Transactional(readOnly = true)
    public void assertChain(Long companyId, Long branchId, Long departmentId, Long designationId) {
        assertChain(companyId, branchId, departmentId, designationId, null);
    }

    /** @param currentBranchId the employee's branch today: keeping an inactive branch is allowed, choosing one is not */
    @Transactional(readOnly = true)
    public void assertChain(Long companyId, Long branchId, Long departmentId, Long designationId, Long currentBranchId) {
        if ((departmentId != null && branchId == null) || (designationId != null && departmentId == null)) {
            throw ApiException.badRequest(EEmployeeError.EMP_023);
        }
        if (branchId != null) {
            Branch b = branches.findByIdAndDeletedFalse(branchId)
                    .filter(x -> Objects.equals(x.getCompanyId(), companyId))
                    .orElseThrow(() -> ApiException.badRequest(EEmployeeError.EMP_002));
            if (b.getStatus() == EBranchStatus.INACTIVE && !Objects.equals(branchId, currentBranchId)) {
                throw ApiException.badRequest(EEmployeeError.EMP_029);
            }
            if (departmentId != null) {
                Department d = departments.findByIdAndDeletedFalse(departmentId)
                        .filter(x -> Objects.equals(x.getBranchId(), b.getId()))
                        .orElseThrow(() -> ApiException.badRequest(EEmployeeError.EMP_003));
                if (designationId != null) {
                    designations.findByIdAndDeletedFalse(designationId)
                            .filter(x -> Objects.equals(x.getDepartmentId(), d.getId()))
                            .orElseThrow(() -> ApiException.badRequest(EEmployeeError.EMP_004));
                }
            }
        }
    }

    /** Document type usable by the company (system or its own). */
    @Transactional(readOnly = true)
    public DocumentType usableDocumentType(Long companyId, Long id) {
        return documentTypes.findByIdAndDeletedFalse(id)
                .filter(t -> t.getCompanyId() == null || Objects.equals(t.getCompanyId(), companyId))
                .orElseThrow(() -> ApiException.badRequest(EEmployeeError.EMP_005));
    }

    /** Id → name, for lists and exports. */
    @Transactional(readOnly = true)
    public Names names(Long companyId, Collection<Long> branchIds, Collection<Long> departmentIds,
                       Collection<Long> designationIds) {
        return new Names(
                toMap(branches.findByCompanyIdAndIdInAndDeletedFalse(companyId, branchIds), Branch::getId, Branch::getName),
                toMap(departments.findByCompanyIdAndIdInAndDeletedFalse(companyId, departmentIds), Department::getId,
                        Department::getName),
                toMap(designations.findByCompanyIdAndIdInAndDeletedFalse(companyId, designationIds), Designation::getId,
                        Designation::getName));
    }

    /** Whole organization of a company, for the import (names → ids). */
    @Transactional(readOnly = true)
    public Tree tree(Long companyId) {
        return new Tree(branches.findByCompanyIdAndDeletedFalseOrderByNameAsc(companyId),
                departments.findByCompanyIdAndDeletedFalseOrderByNameAsc(companyId),
                designations.findByCompanyIdAndDeletedFalseOrderByNameAsc(companyId));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> documentTypeNames(Long companyId) {
        return toMap(documentTypes.visibleTo(companyId), DocumentType::getId, DocumentType::getName);
    }

    public record Names(Map<Long, String> branches, Map<Long, String> departments, Map<Long, String> designations) {
    }

    public record Tree(List<Branch> branches, List<Department> departments, List<Designation> designations) {
    }

    // ------------------------------------------------------------------ helpers

    private static Long company(Long requested) {
        return TenantGuard.resolveCompany(requested, EEmployeeError.EMP_019);
    }

    private Branch requireBranch(Long id) {
        Branch b = branches.findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound(EEmployeeError.EMP_002));
        TenantGuard.assertSameCompany(b.getCompanyId(), EEmployeeError.EMP_002);
        return b;
    }

    private Department requireDepartment(Long id) {
        Department d = departments.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EEmployeeError.EMP_003));
        TenantGuard.assertSameCompany(d.getCompanyId(), EEmployeeError.EMP_003);
        return d;
    }

    private Designation requireDesignation(Long id) {
        Designation d = designations.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EEmployeeError.EMP_004));
        TenantGuard.assertSameCompany(d.getCompanyId(), EEmployeeError.EMP_004);
        return d;
    }

    /** System types (company null) are read-only for everyone, the super admin included: they are seeded. */
    private DocumentType requireOwnDocumentType(Long id) {
        DocumentType t = documentTypes.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ApiException.notFound(EEmployeeError.EMP_005));
        if (t.getCompanyId() == null) {
            throw ApiException.badRequest(EEmployeeError.EMP_021);
        }
        TenantGuard.assertSameCompany(t.getCompanyId(), EEmployeeError.EMP_005);
        return t;
    }

    private static <T> Map<Long, String> toMap(List<T> rows, Function<T, Long> id, Function<T, String> name) {
        return rows.stream().collect(Collectors.toMap(id, name, (a, b) -> a));
    }
}
