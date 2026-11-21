package vn.specom.hrm.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.AccountToken;
import vn.specom.hrm.iam.model.AccountToken.EPurpose;

import java.util.List;
import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {
    Optional<AccountToken> findByTokenHash(String tokenHash);

    List<AccountToken> findByPurposeAndUserIdAndUsedAtIsNull(EPurpose purpose, Long userId);

    List<AccountToken> findByPurposeAndCompanyIdAndUsedAtIsNull(EPurpose purpose, Long companyId);
}
