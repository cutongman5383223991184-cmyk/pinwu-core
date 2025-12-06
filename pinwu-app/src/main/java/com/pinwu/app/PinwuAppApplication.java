package com.pinwu.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.pinwu"})
public class PinwuAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(PinwuAppApplication.class, args);
    }

}
