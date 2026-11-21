package vn.specom.hrm.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.CompanyIpRestriction;

/** The IP restriction switch of each company. */
public interface CompanyIpRestrictionRepository extends JpaRepository<CompanyIpRestriction, Long> {
}
