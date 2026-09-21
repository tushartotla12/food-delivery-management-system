# AI Development Instructions

Food Delivery Management System

## Technology

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- H2
- Flyway
- Spring Security
- Maven

## Development Requirements

The application must support four roles:

- ADMIN
- CUSTOMER
- RESTAURANT_OWNER
- DELIVERY_PARTNER

The application must be state-driven

Orders
- PLACED
- ACCEPTED
- REJECTED
- PREPARING
- READY FOR PICKUP
- PIKCED_UP
- OUT_FOR_DELIVERY
- CANCELLED

Delivery Assignment
- PENDING
- ACCEPTED
- REJECTED
- EXPIRED
- COMPLETED
- CANCELLED

Payment
- PENDING
- CAPTURED
- REFUNDED

Restaurant 
- OPEN
- CLOSED
- INACTIVE


## Development Guidelines

- Use Controller → Service → Repository architecture.
- Use DTOs for API requests/responses.
- Use JPA entities for persistence.
- Use Flyway for production schema migrations.
- Use H2 for tests.
- Add validation and appropriate exception handling.
- Maintain order status history.
- Follow REST conventions.


## Important Business Rules

- Payment must be CAPTURED before an order can move to OUT_FOR_DELIVERY.
- Orders can only be cancelled during the allowed early stages.
- Cancellation must refund a captured payment.
- Delivery assignments can only be created when an order is READY.
- Menu stock must be validated before placing an order.
- Customers can review only completed orders.
- Users can access only resources permitted by their role.


