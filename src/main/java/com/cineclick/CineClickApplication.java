package com.cineclick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CineClickApplication {

    public static void main(String[] args) {
        SpringApplication.run(CineClickApplication.class, args);
    }
}
