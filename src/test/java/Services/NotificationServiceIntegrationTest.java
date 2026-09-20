package Services;

import org.dmg.Dtos.Notification.OrderStatusNotification;
import org.dmg.Entities.Enums.OrderStatus;
import org.dmg.FoodDeliveryManagment;
import org.dmg.Services.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTest(classes = FoodDeliveryManagment.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationServiceIntegrationTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private NotificationService notificationService;

    @MockitoSpyBean
    private NotificationService notificationServiceSpy;

    @Test
    void publishingInsideCommittedTransactionDispatchesNotificationsAfterCommit() {
        clearInvocations(notificationServiceSpy);

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                notificationService.publishOrderStatusChanged(sampleNotification(33L)));

        verify(notificationServiceSpy, timeout(2_000).times(1)).notifyCustomerAfterCommit(any());
        verify(notificationServiceSpy, timeout(2_000).times(1)).notifyRestaurantAfterCommit(any());
        verify(notificationServiceSpy, timeout(2_000).times(1)).notifyDeliveryPartnerAfterCommit(any());
    }

    @Test
    void publishingInsideRolledBackTransactionDoesNotDispatchNotifications() throws InterruptedException {
        clearInvocations(notificationServiceSpy);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.publishOrderStatusChanged(sampleNotification(44L));
            status.setRollbackOnly();
        });

        Thread.sleep(750);

        verify(notificationServiceSpy, times(0)).notifyCustomerAfterCommit(any());
        verify(notificationServiceSpy, times(0)).notifyRestaurantAfterCommit(any());
        verify(notificationServiceSpy, times(0)).notifyDeliveryPartnerAfterCommit(any());
    }

    private OrderStatusNotification sampleNotification(Long orderId) {
        return new OrderStatusNotification(
                orderId,
                10L,
                "Demo Restaurant",
                20L,
                "Owner One",
                30L,
                "Customer One",
                40L,
                "Partner One",
                "221B Baker Street",
                OrderStatus.PREPARING,
                OrderStatus.OUT_FOR_DELIVERY,
                "Status moved for testing",
                LocalDateTime.now(),
                "System"
        );
    }
}


