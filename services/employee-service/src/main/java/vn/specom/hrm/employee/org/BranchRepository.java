package vn.specom.hrm.employee.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByIdAndDeletedFalse(Long id);

    List<Branch> findByCompanyIdAndDeletedFalseOrderByNameAsc(Long companyId);

    List<Branch> findByCompanyIdAndIdInAndDeletedFalse(Long companyId, Collection<Long> ids);

    boolean existsByCompanyIdAndNameAndDeletedFalseAndIdNot(Long companyId, String name, Long id);
}
