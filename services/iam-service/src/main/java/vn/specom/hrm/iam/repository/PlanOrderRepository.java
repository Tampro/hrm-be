package vn.specom.hrm.iam.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.PlanOrder;
import vn.specom.hrm.iam.saas.EOrderStatus;

import java.util.Collection;

public interface PlanOrderRepository extends JpaRepository<PlanOrder, Long> {
    Page<PlanOrder> findByCompanyId(Long companyId, Pageable pageable);

    Page<PlanOrder> findByStatus(EOrderStatus status, Pageable pageable);

    boolean existsByCompanyIdAndStatus(Long companyId, EOrderStatus status);

    long countByCouponIdAndStatusIn(Long couponId, Collection<EOrderStatus> statuses);

    long countByCouponIdAndCompanyIdAndStatusIn(Long couponId, Long companyId, Collection<EOrderStatus> statuses);
}
