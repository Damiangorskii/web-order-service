package com.example.weborderservice.api;

import com.example.weborderservice.model.CreateOrderRequestBody;
import com.example.weborderservice.model.Order;
import com.example.weborderservice.model.PaymentRequest;
import com.example.weborderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("{cartId}")
    public Order createOrder(@PathVariable UUID cartId, @RequestBody @Valid CreateOrderRequestBody requestBody) {
        return orderService.createOrder(cartId, requestBody.customerInfo(), requestBody.deliveryInfo());
    }

    @GetMapping("{orderId}")
    public Order retrieveOrder(@PathVariable UUID orderId) {
        return orderService.retrieveOrder(orderId);
    }

    @DeleteMapping("{orderId}")
    public void deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
    }

    @PostMapping("{orderId}/finalize")
    public Order finalizeOrder(@PathVariable UUID orderId, @RequestBody @Valid PaymentRequest paymentRequest) {
        return orderService.finalizeOrder(orderId);
    }

    @PostMapping("/upload")
    public List<Order> uploadOrders(@RequestPart("file") MultipartFile filePart) {
        return orderService.uploadProducts(filePart);
    }
}
