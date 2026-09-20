package Controllers.Admin;

import Controllers.IntegrationTestBase;
import org.dmg.Entities.City;
import org.dmg.Entities.Enums.Role;
import org.dmg.Entities.User;
import org.dmg.FoodDeliveryManagment;
import org.dmg.Repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FoodDeliveryManagment.class)
class AdminControllerValidationIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @BeforeEach
    void setUp() {
        cityRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@dmg.com");
        admin.setPassword("admin123");
        admin.setPhone("1000000000");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
    }

    @Test
    void createCityRejectsBlankNameAtControllerBoundary() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/admin/cities")))
                .header("Authorization", adminBasicAuth())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"   \",\"active\":true}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(cityRepository.count()).isZero();
    }

    @Test
    void updateCityRejectsBlankNameAtControllerBoundary() throws Exception {
        City city = City.builder()
                .name("Hyderabad")
                .active(true)
                .build();
        city = cityRepository.save(city);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/admin/cities/" + city.getId())))
                .header("Authorization", adminBasicAuth())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"\",\"active\":false}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(cityRepository.findById(city.getId()))
                .get()
                .extracting(City::getName, City::getActive)
                .containsExactly("Hyderabad", true);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String adminBasicAuth() {
        String token = "admin@dmg.com:admin123";
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
