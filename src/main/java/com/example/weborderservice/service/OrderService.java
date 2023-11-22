package com.example.weborderservice.service;

import com.example.weborderservice.client.ShoppingClient;
import com.example.weborderservice.model.CustomerInfo;
import com.example.weborderservice.model.DeliveryInfo;
import com.example.weborderservice.model.Order;
import com.example.weborderservice.model.ShoppingCart;
import com.example.weborderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    public static final String ORDER_NOT_FOUND = "Order not found";
    private final OrderRepository orderRepository;
    private final ShoppingClient shoppingClient;
    private final ObjectMapper objectMapper;

    public Order createOrder(final UUID cartId, final CustomerInfo customerInfo, final DeliveryInfo deliveryInfo) {
        ShoppingCart shoppingCart = shoppingClient.getShoppingCart(cartId);
        Order order = new Order(
                UUID.randomUUID(),
                shoppingCart.getProducts(),
                customerInfo,
                deliveryInfo,
                false
        );
        return orderRepository.save(order);
    }

    public Order retrieveOrder(final UUID orderId) {
        return orderRepository.findOrderByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));
    }

    public void deleteOrder(final UUID orderId) {
        Order order = orderRepository.findOrderByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));
        orderRepository.deleteOrderByOrderId(order.getOrderId());
    }

    public Order finalizeOrder(final UUID orderId) {
        Order order = orderRepository.findOrderByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));
        payForOrder(order);
        return orderRepository.save(order);
    }

    public List<Order> uploadProducts(final MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            List<Order> orders = objectMapper.readValue(bytes, new TypeReference<>() {});
            orders.forEach(this::setOrderId);
            return orderRepository.saveAll(orders);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file", e);
        }
    }

    private void payForOrder(final Order order) {
        if (!order.isPaid()) {
            order.setPaid(true);
        }
    }

    private void setOrderId(final Order order) {
        order.setOrderId(UUID.randomUUID());
    }
}
