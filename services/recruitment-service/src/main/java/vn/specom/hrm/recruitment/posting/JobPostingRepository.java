package vn.specom.hrm.recruitment.posting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    /** keyword matches title or job code; the caller escapes % and _ with !. */
    @Query("select p from JobPosting p where p.companyId = :companyId and p.deleted = false "
            + "and (:status is null or p.status = :status) and (:published is null or p.published = :published) "
            + "and (:kw = '' or lower(p.title) like :kw escape '!' or lower(p.jobCode) like :kw escape '!')")
    Page<JobPosting> search(Long companyId, JobPosting.EStatus status, Boolean published, String kw, Pageable pageable);

    List<JobPosting> findByCompanyIdAndDeletedFalseOrderByTitleAsc(Long companyId);

    Optional<JobPosting> findByIdAndDeletedFalse(Long id);

    boolean existsByJobTypeIdAndDeletedFalse(Long jobTypeId);

    boolean existsByLocationIdAndDeletedFalse(Long locationId);

    @Query(value = "SELECT next_value FROM recruitment_sequence WHERE company_id = :companyId AND name = :name FOR UPDATE",
            nativeQuery = true)
    Long lockSequence(Long companyId, String name);

    @Modifying
    @Query(value = "INSERT IGNORE INTO recruitment_sequence (company_id, name, next_value) VALUES (:companyId, :name, 1)",
            nativeQuery = true)
    void ensureSequence(Long companyId, String name);

    @Modifying
    @Query(value = "UPDATE recruitment_sequence SET next_value = next_value + 1 WHERE company_id = :companyId "
            + "AND name = :name", nativeQuery = true)
    void bumpSequence(Long companyId, String name);
}
