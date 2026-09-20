package org.dmg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FoodDeliveryManagment {
    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryManagment.class, args);
    }
}