package vn.specom.hrm.employee.org;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.specom.hrm.employee.common.BaseAuditableEntity;

/** company_id null = system type (seeded: Identity Proof, Address Proof, Educational Certificates), read-only. */
@Getter
@Setter
@Entity
@Table(name = "document_type")
public class DocumentType extends BaseAuditableEntity {
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultSlot;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
