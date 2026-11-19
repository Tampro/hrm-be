package vn.specom.hrm.employee.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.specom.hrm.employee.common.BaseAuditableEntity;

/** company_id null = system category (seeded), read-only. */
@Getter
@Setter
@Entity
@Table(name = "announcement_category")
public class AnnouncementCategory extends BaseAuditableEntity {
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Tag colour, #RRGGBB. */
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
