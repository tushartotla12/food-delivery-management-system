package org.dmg.Config;

import lombok.RequiredArgsConstructor;
import org.dmg.Entities.Enums.Role;
import org.dmg.Entities.User;
import org.dmg.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        createUser("Admin User", "admin@dmg.com", "admin123", "1000000000", Role.ADMIN);
        createUser("Owner User", "owner@dmg.com", "owner123", "1000000001", Role.RESTAURANT_OWNER);
        createUser("Customer User", "customer@dmg.com", "customer123", "1000000002", Role.CUSTOMER);
        createUser("Partner User", "partner@dmg.com", "partner123", "1000000003", Role.DELIVERY_PARTNER);
    }

    private void createUser(String name, String email, String password, String phone, Role role) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(password)
                .phone(phone)
                .role(role)
                .active(true).build();
        userRepository.save(user);
    }
}

