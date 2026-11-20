package vn.specom.hrm.employee.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRatingRepository extends JpaRepository<ReviewRating, Long> {
    List<ReviewRating> findByReviewIdOrderByCategoryNameAscIndicatorNameAsc(Long reviewId);
}
