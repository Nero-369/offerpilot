package com.offerpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class OfferPilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(OfferPilotApplication.class, args);
    }
}
