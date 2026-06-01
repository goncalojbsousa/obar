package com.obar.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ObarWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObarWebApplication.class, args);
    }

}
