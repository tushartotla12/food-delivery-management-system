package Controllers;

import org.dmg.FoodDeliveryManagment;
import org.dmg.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = FoodDeliveryManagment.class
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class IntegrationTestBase {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CityRepository cityRepository;

    @Autowired
    protected RestaurantRepository restaurantRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected DeliveryPartnerRepository deliveryPartnerRepository;

    @Autowired
    protected DeliveryAssignmentRepository deliveryAssignmentRepository;

    @BeforeEach
    void cleanDatabase() {
        deliveryAssignmentRepository.deleteAll();
        orderRepository.deleteAll();
        deliveryPartnerRepository.deleteAll();
        restaurantRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }
}