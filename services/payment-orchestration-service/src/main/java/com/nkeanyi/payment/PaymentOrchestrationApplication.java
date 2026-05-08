package com.nkeanyi.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentOrchestrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestrationApplication.class, args);
    }
}
