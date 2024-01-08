package com.example.weborderservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@AllArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 * * * * *")
    public void cleanUpOldOrders() {
        orderService.deleteOldOrders();
        log.info("Remove old orders");
    }
}
