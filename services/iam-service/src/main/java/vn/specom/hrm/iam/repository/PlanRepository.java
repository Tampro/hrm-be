package vn.specom.hrm.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByIdAndDeletedFalse(Long id);

    Optional<Plan> findFirstByDefaultPlanTrueAndDeletedFalse();

    List<Plan> findByDeletedFalseOrderByMonthlyPriceAsc();

    List<Plan> findByEnabledTrueAndDeletedFalseOrderByMonthlyPriceAsc();

    boolean existsByCodeAndDeletedFalse(String code);
}
