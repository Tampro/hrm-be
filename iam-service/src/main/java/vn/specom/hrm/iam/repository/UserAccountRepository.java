package vn.specom.hrm.iam.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.specom.hrm.iam.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** One query for the authority resolver, which runs on every request (behind a short cache). */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserAccount> findByKeycloakIdAndDeletedFalse(String keycloakId);

    Optional<UserAccount> findByIdAndDeletedFalse(Long id);

    boolean existsByUsernameAndDeletedFalse(String username);

    @Query("select u from UserAccount u where u.companyId = :companyId and u.deleted = false and "
            + "(:keyword = '' or lower(u.username) like lower(concat('%', :keyword, '%')) "
            + "or lower(u.fullName) like lower(concat('%', :keyword, '%')) "
            + "or lower(u.email) like lower(concat('%', :keyword, '%')))")
    Page<UserAccount> search(Long companyId, String keyword, Pageable pageable);

    @Query("select count(u) > 0 from UserAccount u join u.roles r where r.id = :roleId and u.deleted = false")
    boolean existsActiveUserWithRole(Long roleId);
}
