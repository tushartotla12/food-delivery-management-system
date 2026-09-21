# FoodDeliveryManagment

A Spring Boot-based food delivery management backend that supports role-based workflows for admins, restaurant owners, customers, and delivery partners.

## Overview

This project implements a complete order lifecycle for a food delivery platform:

- admins manage master data such as cities and users
- customers browse restaurants and menu items, place orders, pay, cancel orders, and submit reviews
- restaurant owners manage menu items and order preparation flow
- delivery partners accept assignments and update delivery progress

The application uses Spring Security with HTTP Basic authentication and role-based access control.

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- Flyway
- PostgreSQL for the main database
- H2** for tests
- Lombok
- Maven Wrapper

## Features

### Admin

- create and manage cities
- create and manage application users
- manage role-based user records

### Customer

- list active cities
- browse restaurants by city
- browse menu items by restaurant
- place orders
- capture payment before delivery
- cancel placed orders
- submit restaurant and partner reviews after delivery
- view order details and timeline

### Restaurant Owner

- add and update menu items
- toggle menu availability
- adjust stock
- accept or reject orders
- move orders through preparation states
- create delivery assignments when orders are ready for pickup

### Delivery Partner

- view pending assignments
- accept or reject delivery assignments
- mark orders as picked up
- move orders to out-for-delivery
- mark orders as delivered
- update availability

### Order and Payment Flow

- order starts in `PLACED`
- payment starts in `PENDING`
- customer can capture payment through the customer payment endpoint
- delivery cannot move to `OUT_FOR_DELIVERY` unless payment is captured
- if a customer cancels an order, the payment moves to `REFUNDED`
- on successful delivery, the order moves to `DELIVERED`


### Notification Flow
- change in order state publishes the notifcation events
- listeners capture the events (prints logs here to verify event is listened and captured)

## Project Structure

- `controllers/` - REST APIs for each role
- `services/` - business rules and workflow logic
- `entities/` - JPA entities
- `repositories/` - database access layer
- `dto/` - request and response models
- `security/` - authentication and authorization
- `config/` - startup/configuration components
- `events/` - domain events used for notifications
- `db/migration/` - Flyway schema migration scripts

## Security

The app uses:

- HTTP Basic authentication
- role-based URL protection
- stateless session policy
- in-memory password handling via `NoOpPasswordEncoder` in the current implementation

### Protected API Paths

- `/api/admin/**` - `ADMIN`
- `/api/owner/**` - `RESTAURANT_OWNER`
- `/api/customers/**` - `CUSTOMER`
- `/api/partners/**` - `DELIVERY_PARTNER`

## Demo Users

The app seeds demo users automatically on first run when the database is empty.
Seeded credentials are stored as plain text in the current implementation because `NoOpPasswordEncoder` is used.


## Project Configuration

### Main Application

The entry point is:

- `src/main/java/com/dmg/fooddeliverymanagment/FoodDeliveryManagmentApplication.java`

### Main Database Settings

The application reads PostgreSQL settings from environment variables in `src/main/resources/application.properties`:

- `DB_HOST` - default `localhost`
- `DB_PORT` - default `5432`
- `DB_NAME` - default `food_delivery_management`
- `DB_USERNAME` - default `postgres`
- `DB_PASSWORD` - default `postgres`

### JPA / Flyway Behavior

- production-like runtime uses `spring.jpa.hibernate.ddl-auto=validate`
- schema is created and maintained through Flyway migrations
- primary migration file is `src/main/resources/db/migration/V1__init_schema.sql`
- `open-in-view` is disabled

## Getting Started

### Prerequisites

- Java 17
- PostgreSQL running locally - docker container or driver installation locally
- Maven Wrapper available in the repository

### Run the Application

On Windows:

.\mvnw.cmd spring-boot:run

On macOS/Linux:

./mvnw spring-boot:run

### Build the Project

On Windows:

.\mvnw.cmd clean package

On macOS/Linux:

./mvnw clean package


## Testing

The test suite includes unit tests and integration tests for the core flows.

### Test Database

Tests use:

- H2 in-memory database
- Flyway disabled in the test profile
- `ddl-auto=create-drop`

### Run Tests

On Windows:

.\mvnw.cmd test

On macOS/Linux:
./mvnw test

## API Notes

### Base URLs

- `/api/admin`
- `/api/customers`
- `/api/owner`
- `/api/partners`

### Important Customer Flows

- place order: `POST /api/customers/orders`
- capture payment: `PATCH /api/customers/orders/{orderId}/payment`
- cancel order: `PATCH /api/customers/orders/{orderId}/cancel`
- submit review: `POST /api/customers/orders/{orderId}/rating`

##BASIC LIFE CYCLE  FLOW OF ORDER

customer browses city and chooses one
customer browses restaurant from choosen city and chooses one
customer browses menu item of chosen restaurant
customer places the order
restaurant owner accepts/rejects order
restaurant owner changes the state of order to preparing
restaurant owner changes the state of order to ready to pickup - delivery assignment gets created at this step
delivery partner accepts/rejects the delivery assignment
delivery partner changes the state of delivery assignment to picked up
delivery partner changes the state of delivery assignment to out for delivery
customer completes the payment for the order changing it state to captured from pending
delivery partner changes the state of delivery assignment to delivered


###Important Note
Validations of state is done at every level - placed order cannot be pickedup before accepted and so on.
Before an order can be moved to delivery, payment must already be captured. The delivery partner flow rejects unpaid orders.


