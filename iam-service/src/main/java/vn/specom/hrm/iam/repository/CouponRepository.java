package vn.specom.hrm.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.specom.hrm.iam.model.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByIdAndDeletedFalse(Long id);

    Optional<Coupon> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Coupon> findByDeletedFalseOrderByIdDesc();
}
