package Controllers.Customer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import Controllers.IntegrationTestBase;
import org.dmg.FoodDeliveryManagment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.*;
import org.dmg.Repositories.*;
import java.math.BigDecimal;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;


import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FoodDeliveryManagment.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerControllerIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    private PaymentRepository paymentRepository;

    @Test
    void listCitiesReturnsPersistedCitiesForCustomer() throws Exception {
        User customer = userRepository.save(createUser("customer@dmg.com", "customer123", Role.CUSTOMER));

        City city = new City();
        city.setName("Hyderabad");
        city.setActive(true);
        cityRepository.save(city);

        var response = restTemplate.withBasicAuth(customer.getEmail(), "customer123")
                .getForEntity(baseUrl("/api/customers/cities"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode payload = objectMapper.readTree(response.getBody());
        assertThat(payload).hasSize(1);
        assertThat(payload.get(0).get("name").asText()).isEqualTo("Hyderabad");
        assertThat(payload.get(0).get("active").asBoolean()).isTrue();
    }

    @Test
    void placeOrderCreatesOrderAndDecrementsStockThroughHttpEndpoint() throws Exception {
        User customer = userRepository.save(createUser("customer@dmg.com", "customer123", Role.CUSTOMER));
        User owner = userRepository.save(createUser("owner@dmg.com", "owner123", Role.RESTAURANT_OWNER));

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

        MenuItem menuItem = new MenuItem();
        menuItem.setName("Chicken Biryani");
        menuItem.setDescription("Spicy");
        menuItem.setPrice(new BigDecimal("10.00"));
        menuItem.setStockQuantity(5);
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        menuItem = menuItemRepository.save(menuItem);

        String requestJson = """
                {
                  "restaurantId": %d,
                  "deliveryAddress": "  221B Baker Street  ",
                  "paymentMethod": "COD",
                  "items": [
                    {"menuItemId": %d, "quantity": 2}
                  ]
                }
                """.formatted(restaurant.getId(), menuItem.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        var response = restTemplate.withBasicAuth(customer.getEmail(), "customer123")
                .exchange(baseUrl("/api/customers/orders"), HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode payload = objectMapper.readTree(response.getBody());
        assertThat(payload.get("restaurantId").asLong()).isEqualTo(restaurant.getId());
        assertThat(payload.get("status").asText()).isEqualTo(OrderStatus.PLACED.name());
        assertThat(payload.get("paymentStatus").asText()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(payload.get("deliveryAddress").asText()).isEqualTo("221B Baker Street");
        assertThat(payload.get("totalAmount").asText()).isEqualTo("20.0");

        assertThat(menuItemRepository.findById(menuItem.getId()))
                .get()
                .extracting(MenuItem::getStockQuantity)
                .isEqualTo(3);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void capturePaymentUpdatesOrderPaymentStatusThroughHttpEndpoint() throws Exception {
        User customer = userRepository.save(createUser("customer@dmg.com", "customer123", Role.CUSTOMER));
        User owner = userRepository.save(createUser("owner@dmg.com", "owner123", Role.RESTAURANT_OWNER));

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

        MenuItem menuItem = new MenuItem();
        menuItem.setName("Chicken Biryani");
        menuItem.setDescription("Spicy");
        menuItem.setPrice(new BigDecimal("10.00"));
        menuItem.setStockQuantity(5);
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        menuItem = menuItemRepository.save(menuItem);

        String requestJson = """
                {
                  "restaurantId": %d,
                  "deliveryAddress": "221B Baker Street",
                  "paymentMethod": "COD",
                  "items": [
                    {"menuItemId": %d, "quantity": 2}
                  ]
                }
                """.formatted(restaurant.getId(), menuItem.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        var createResponse = restTemplate.withBasicAuth(customer.getEmail(), "customer123")
                .exchange(baseUrl("/api/customers/orders"), HttpMethod.POST, entity, String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createdOrder = objectMapper.readTree(createResponse.getBody());
        long orderId = createdOrder.get("orderId").asLong();

        HttpEntity<Void> paymentEntity = new HttpEntity<>(new HttpHeaders());
        var paymentResponse = restTemplate.withBasicAuth(customer.getEmail(), "customer123")
                .exchange(baseUrl("/api/customers/orders/" + orderId + "/payment"), HttpMethod.PATCH, paymentEntity, String.class);

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode paymentPayload = objectMapper.readTree(paymentResponse.getBody());
        assertThat(paymentPayload.get("paymentStatus").asText()).isEqualTo(PaymentStatus.CAPTURED.name());
        assertThat(orderRepository.findById(orderId))
                .get()
                .extracting(Order::getPaymentStatus)
                .isEqualTo(PaymentStatus.CAPTURED);
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
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
}