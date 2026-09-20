package Services;

import org.dmg.Dtos.Admin.CityRequest;
import org.dmg.Dtos.Admin.CreateUserRequest;
import org.dmg.Exception.ConflictException;
import org.dmg.Services.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.dmg.Entities.Enums.*;
import org.dmg.Entities.*;
import org.dmg.Repositories.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CityRepository cityRepository;


    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createCityTrimsNameAndDefaultsActiveToTrue() {
        when(cityRepository.existsByName("Pune")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> {
            City city = invocation.getArgument(0);
            city.setId(1L);
            return city;
        });

        City result = adminService.createCity(new CityRequest("  Pune  ", null));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Pune");
        assertThat(result.getActive()).isTrue();
        verify(cityRepository).save(any(City.class));
    }

    @Test
    void updateCityTrimsNameAndPreservesActiveWhenNotProvided() {
        City existingCity = new City();
        existingCity.setId(10L);
        existingCity.setName("Mumbai");
        existingCity.setActive(true);

        when(cityRepository.findById(10L)).thenReturn(Optional.of(existingCity));
        when(cityRepository.existsByName("Pune")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));

        City result = adminService.updateCity(10L, new CityRequest("  Pune  ", null));

        assertThat(result.getName()).isEqualTo("Pune");
        assertThat(result.getActive()).isTrue();
        verify(cityRepository).save(existingCity);
    }

    @Test
    void createUserEncodesPasswordAndDefaultsActiveToTrue() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("9999999999")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(25L);
            return user;
        });

        User result = adminService.createUser(new CreateUserRequest(
                "  Alice  ",
                "alice@example.com",
                "9999999999",
                "secret",
                Role.ADMIN,
                null));

        assertThat(result.getId()).isEqualTo(25L);
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-secret");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getActive()).isTrue();
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void createCityRejectsDuplicateNames() {
        when(cityRepository.existsByName("Pune")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createCity(new CityRequest("Pune", true)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("City already exists");

        verify(cityRepository, never()).save(any());
    }
}

