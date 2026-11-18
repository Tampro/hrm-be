package vn.specom.hrm.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.Permission;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByCodeIn(Collection<String> codes);

    List<Permission> findAllByOrderByModuleAscCodeAsc();
}
