package com.example.weborderservice.model;

import jakarta.validation.Valid;

public record CreateOrderRequestBody(@Valid CustomerInfo customerInfo, @Valid DeliveryInfo deliveryInfo) {
}
