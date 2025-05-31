package com.amazon.pricenotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceNotifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriceNotifierApplication.class, args);
    }
} 