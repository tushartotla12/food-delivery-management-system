package org.dmg.Dtos.Customer;

public record RatingReviewRequest(Integer restaurantRating,
                                  Integer partnerRating,
                                  String reviewComment) {
}


