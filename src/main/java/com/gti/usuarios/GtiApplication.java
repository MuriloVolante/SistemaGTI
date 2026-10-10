package com.gti.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GtiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GtiApplication.class, args);
    }
}