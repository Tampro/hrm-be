package vn.specom.hrm.employee.org;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.specom.hrm.employee.common.BaseAuditableEntity;

@Getter
@Setter
@Entity
@Table(name = "designation")
public class Designation extends BaseAuditableEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
