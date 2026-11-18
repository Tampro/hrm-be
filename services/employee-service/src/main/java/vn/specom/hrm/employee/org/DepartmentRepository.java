package vn.specom.hrm.employee.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByIdAndDeletedFalse(Long id);

    List<Department> findByCompanyIdAndDeletedFalseOrderByNameAsc(Long companyId);

    List<Department> findByCompanyIdAndBranchIdAndDeletedFalseOrderByNameAsc(Long companyId, Long branchId);

    List<Department> findByCompanyIdAndIdInAndDeletedFalse(Long companyId, Collection<Long> ids);

    boolean existsByBranchIdAndNameAndDeletedFalseAndIdNot(Long branchId, String name, Long id);

    boolean existsByBranchIdAndDeletedFalse(Long branchId);
}
