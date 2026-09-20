package org.dmg.Repositories;

import org.dmg.Entities.RatingReview;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RatingReviewRepository extends JpaRepository<RatingReview, Long> {


    boolean existsByOrder_Id(Long orderId);
}

