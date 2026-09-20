package Controllers.Partner;

import Controllers.IntegrationTestBase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.*;
import org.dmg.FoodDeliveryManagment;
import org.dmg.Repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FoodDeliveryManagment.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PartnerControllerIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Autowired
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @SuppressWarnings("resource")
    @Test
    void acceptAssignmentUpdatesAssignmentAndPartnerState() throws Exception {
        User customer = userRepository.save(createUser("customer@dmg.com", "customer123", Role.CUSTOMER));
        User owner = userRepository.save(createUser("owner@dmg.com", "owner123", Role.RESTAURANT_OWNER));
        User partnerUser = userRepository.save(createUser("partner@dmg.com", "partner123", Role.DELIVERY_PARTNER));

        City city = new City();
        city.setName("Hyderabad");
        city.setActive(true);
        city = cityRepository.save(city);

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setCity(city);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);
        restaurant = restaurantRepository.save(restaurant);

        DeliveryPartner partner = new DeliveryPartner();
        partner.setUser(partnerUser);
        partner.setCity(city);
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.AVAILABLE);
        partner.setActive(true);
        partner = deliveryPartnerRepository.save(partner);

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCity(city);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setDeliveryAddress("221B Baker Street");
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));
        order.setTotalAmount(new BigDecimal("20.0"));
        order = orderRepository.save(order);


        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(order);
        assignment.setStatus(AssignmentStatus.PENDING);
        assignment.setAssignedAt(LocalDateTime.of(2026, 9, 19, 10, 5));
        assignment = deliveryAssignmentRepository.save(assignment);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/partners/assignments/" + order.getId() + "/accept")))
                .header("Authorization", basicAuth())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload.get("orderId").asLong()).isEqualTo(order.getId());
        assertThat(payload.get("deliveryPartnerId").asLong()).isEqualTo(partner.getId());
        assertThat(payload.get("status").asText()).isEqualTo(AssignmentStatus.ACCEPTED.name());

        assertThat(deliveryPartnerRepository.findById(partner.getId()))
                .get()
                .extracting(DeliveryPartner::getAvailabilityStatus)
                .isEqualTo(PartnerAvailabilityStatus.BUSY);
        assertThat(deliveryAssignmentRepository.findById(assignment.getId()))
                .get()
                .extracting(DeliveryAssignment::getStatus)
                .isEqualTo(AssignmentStatus.ACCEPTED);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String basicAuth() {
        String token = "partner@dmg.com:partner123";
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private User createUser(String email, String password, Role role) {
        User user = new User();
        user.setName(email.substring(0, email.indexOf('@')));
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(email.replace("@dmg.com", "").replace("customer", "7000000001").replace("owner", "7000000002").replace("partner", "7000000003"));
        user.setRole(role);
        user.setActive(true);
        return user;
    }

}