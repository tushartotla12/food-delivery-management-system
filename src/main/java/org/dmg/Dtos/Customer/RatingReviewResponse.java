package org.dmg.Dtos.Customer;

public record RatingReviewResponse(Long reviewId,
                                   Long orderId,
                                   Long customerUserId,
                                   Integer restaurantRating,
                                   Integer partnerRating,
                                   String reviewComment) {
}

