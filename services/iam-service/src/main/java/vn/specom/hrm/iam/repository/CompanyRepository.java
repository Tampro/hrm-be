package vn.specom.hrm.iam.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.specom.hrm.iam.constant.ECompanyStatus;
import vn.specom.hrm.iam.model.Company;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByIdAndDeletedFalse(Long id);

    boolean existsByCodeAndDeletedFalse(String code);

    Page<Company> findByDeletedFalse(Pageable pageable);

    String FILTER = "c.deleted = false"
            + " and (:keyword = '' or lower(c.name) like lower(concat('%', :keyword, '%'))"
            + "      or lower(c.code) like lower(concat('%', :keyword, '%'))"
            + "      or lower(coalesce(c.email, '')) like lower(concat('%', :keyword, '%')))"
            + " and c.createdDate >= :from and c.createdDate < :to";

    /** Companies list: keyword on name/code/email, created-date range [from, to), optional status. */
    @Query("select c from Company c where " + FILTER + " and (:status is null or c.status = :status)")
    Page<Company> search(String keyword, Instant from, Instant to, ECompanyStatus status, Pageable pageable);

    /** Status tab counts with the same keyword/date filter: rows of [status, count]. */
    @Query("select c.status, count(c) from Company c where " + FILTER + " group by c.status")
    List<Object[]> countByStatus(String keyword, Instant from, Instant to);
}
