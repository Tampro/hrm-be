package vn.specom.hrm.employee.employee;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.specom.hrm.employee.employee.EmployeeDtos.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class EmployeeSpecs {
    private EmployeeSpecs() {
    }

    /** @param withStatus false for the status tabs, which count every status under the other filters */
    static Specification<Employee> of(Long companyId, Filter f, boolean withStatus) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("companyId"), companyId));
            p.add(cb.isFalse(root.get("deleted")));
            if (f.keyword() != null && !f.keyword().isBlank()) {
                String like = "%" + escape(f.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("employeeNo")), like, '\\'),
                        cb.like(cb.lower(root.get("employeeCode")), like, '\\')));
            }
            if (f.branchId() != null) {
                p.add(cb.equal(root.get("branchId"), f.branchId()));
            }
            if (f.departmentId() != null) {
                p.add(cb.equal(root.get("departmentId"), f.departmentId()));
            }
            if (f.designationId() != null) {
                p.add(cb.equal(root.get("designationId"), f.designationId()));
            }
            if (withStatus && f.status() != null) {
                p.add(cb.equal(root.get("status"), f.status()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** A keyword is text, not a pattern: % and _ match themselves. */
    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
