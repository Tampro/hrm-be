package vn.specom.hrm.employee.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
    Optional<Designation> findByIdAndDeletedFalse(Long id);

    List<Designation> findByCompanyIdAndDeletedFalseOrderByNameAsc(Long companyId);

    List<Designation> findByCompanyIdAndDepartmentIdAndDeletedFalseOrderByNameAsc(Long companyId, Long departmentId);

    List<Designation> findByCompanyIdAndIdInAndDeletedFalse(Long companyId, Collection<Long> ids);

    boolean existsByDepartmentIdAndNameAndDeletedFalseAndIdNot(Long departmentId, String name, Long id);

    boolean existsByDepartmentIdAndDeletedFalse(Long departmentId);
}
