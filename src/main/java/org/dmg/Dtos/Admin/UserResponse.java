package org.dmg.Dtos.Admin;


import org.dmg.Entities.Enums.Role;

public record UserResponse(Long id,
                           String name,
                           String email,
                           String phone,
                           Role role,
                           Boolean active) {
}

