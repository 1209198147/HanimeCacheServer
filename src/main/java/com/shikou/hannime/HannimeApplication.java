package com.shikou.hannime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HannimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HannimeApplication.class, args);
    }

}
