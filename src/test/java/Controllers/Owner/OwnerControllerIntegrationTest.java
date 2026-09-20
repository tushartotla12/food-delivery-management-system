package Controllers.Owner;


import Controllers.IntegrationTestBase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.*;
import org.dmg.FoodDeliveryManagment;
import org.dmg.Repositories.*;
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
@SuppressWarnings("resource")
class OwnerControllerIntegrationTest extends IntegrationTestBase {

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
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Test
    void addMenuItemCreatesMenuItemForOwnedRestaurant() throws Exception {
        User owner = userRepository.save(createUser("owner@dmg.com", "owner123", Role.RESTAURANT_OWNER));
        Restaurant restaurant = restaurantRepository.save(createRestaurant(owner));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/owner/restaurants/" + restaurant.getId() + "/menu-items")))
                .header("Authorization", ownerBasicAuth())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "  Chicken Biryani  ",
                          "description": "  spicy  ",
                          "price": 12.50,
                          "stockQuantity": 8,
                          "available": true
                        }
                        """))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = objectMapper.readTree(response.body());
        assertThat(payload.get("restaurantId").asLong()).isEqualTo(restaurant.getId());
        assertThat(payload.get("name").asText()).isEqualTo("Chicken Biryani");
        assertThat(payload.get("description").asText()).isEqualTo("spicy");
        assertThat(payload.get("price").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(payload.get("stockQuantity").asInt()).isEqualTo(8);
        assertThat(payload.get("available").asBoolean()).isTrue();
        assertThat(menuItemRepository.count()).isEqualTo(1);
    }

    @Test
    void acceptPrepareAndReadyOrderUpdatesOrderAndCreatesAssignment() throws Exception {
        User owner = userRepository.save(createUser("owner@dmg.com", "owner123", Role.RESTAURANT_OWNER));
        User customer = userRepository.save(createUser("customer@dmg.com", "customer123", Role.CUSTOMER));
        Restaurant restaurant = restaurantRepository.save(createRestaurant(owner));
        Order order = orderRepository.save(createPlacedOrder(customer, restaurant));

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest acceptRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/owner/restaurants/" + restaurant.getId() + "/orders/" + order.getId() + "/accept")))
                .header("Authorization", ownerBasicAuth())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> acceptResponse = client.send(acceptRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(acceptResponse.statusCode()).isEqualTo(200);

        HttpRequest prepareRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/owner/restaurants/" + restaurant.getId() + "/orders/" + order.getId() + "/prepare")))
                .header("Authorization", ownerBasicAuth())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> prepareResponse = client.send(prepareRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(prepareResponse.statusCode()).isEqualTo(200);

        HttpRequest readyRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/owner/restaurants/" + restaurant.getId() + "/orders/" + order.getId() + "/ready-for-pickup")))
                .header("Authorization", ownerBasicAuth())
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> readyResponse = client.send(readyRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(readyResponse.statusCode()).isEqualTo(200);

        JsonNode readyPayload = objectMapper.readTree(readyResponse.body());
        assertThat(readyPayload.get("status").asText()).isEqualTo(OrderStatus.READY_FOR_PICKUP.name());
        assertThat(readyPayload.get("paymentStatus").asText()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(readyPayload.get("deliveryAddress").asText()).isEqualTo("221B Baker Street");

        assertThat(orderRepository.findById(order.getId()))
                .get()
                .extracting(Order::getStatus)
                .isEqualTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(deliveryAssignmentRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(DeliveryAssignment::getStatus)
                .isEqualTo(AssignmentStatus.PENDING);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String ownerBasicAuth() {
        String token = "owner@dmg.com:owner123";
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private User createUser(String email, String password, Role role) {
        User user = new User();
        user.setName(email.substring(0, email.indexOf('@')));
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(email.replace("@dmg.com", "").replace("customer", "7000000001").replace("owner", "7000000002"));
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Restaurant createRestaurant(User owner) {
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
        return restaurant;
    }

    private Order createPlacedOrder(User customer, Restaurant restaurant) {
        MenuItem menuItem = new MenuItem();
        menuItem.setName("Chicken Biryani");
        menuItem.setDescription("Spicy");
        menuItem.setPrice(new BigDecimal("12.50"));
        menuItem.setStockQuantity(10);
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        menuItemRepository.save(menuItem);

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCity(restaurant.getCity());
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(new BigDecimal("12.50"));
        order.setDeliveryAddress("221B Baker Street");
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));
        return order;
    }
}


