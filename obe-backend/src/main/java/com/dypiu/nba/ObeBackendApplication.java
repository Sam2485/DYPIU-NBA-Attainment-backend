package com.dypiu.nba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ObeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObeBackendApplication.class, args);
    }
}
