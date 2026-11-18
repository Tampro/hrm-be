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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ECompanyStatus status;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
