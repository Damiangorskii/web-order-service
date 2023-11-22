package com.example.weborderservice.repository;

import com.example.weborderservice.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findOrderByOrderId(UUID orderId);

    void deleteOrderByOrderId(UUID orderId);
}
