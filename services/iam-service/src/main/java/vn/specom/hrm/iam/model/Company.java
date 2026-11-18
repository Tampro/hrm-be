package vn.specom.hrm.iam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.specom.hrm.iam.constant.ECompanyStatus;

/** Tenant. Every company-owned row in every service carries its id as {@code company_id}. */
@Getter
@Setter
@Entity
@Table(name = "company")
public class Company extends BaseAuditableEntity {
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** Contact email of the company (not a login). Null for companies created before it existed. */
    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ECompanyStatus status;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    /** Current plan; null = no subscription (internal / bootstrap company): no plan limits, never expires. */
    @Column(name = "plan_id")
    private Long planId;

    /** Null with a plan = does not expire (default plan). */
    @Column(name = "plan_expires_at")
    private java.time.Instant planExpiresAt;

    @Column(name = "on_trial", nullable = false)
    private boolean onTrial;

    /** This company's code for referring others: REF + 6-digit id (docs/analysis/06 §3.2.6). */
    @Column(name = "referral_code", length = 20)
    private String referralCode;

    /** The company whose code was given at creation; null = not referred. */
    @Column(name = "referred_by_company_id")
    private Long referredByCompanyId;
}
