package org.dmg.Dtos.Admin;

import jakarta.validation.constraints.NotBlank;

public record CityRequest(
        @NotBlank(message = "City name is required")
        String name,
        Boolean active) {
}


