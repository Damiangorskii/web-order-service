package com.example.weborderservice.service;


import com.example.weborderservice.client.ShoppingClient;
import com.example.weborderservice.model.*;
import com.example.weborderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private static final CustomerInfo CUSTOMER_INFO = CustomerInfo.builder()
            .firstName("Joe")
            .lastName("Doe")
            .email("joedoe@test.com")
            .phoneNumber("555666777")
            .build();
    private static final DeliveryInfo DELIVERY_INFO = DeliveryInfo.builder()
            .address("Street 1")
            .city("London")
            .postalCode("33333")
            .country("United Kingdom")
            .build();

    private static final Order ORDER = Order.builder()
            .orderId(UUID.randomUUID())
            .products(List.of(Product.builder()
                    .id(UUID.randomUUID())
                    .name("Test product")
                    .description("Test description")
                    .price(BigDecimal.TEN)
                    .manufacturer(Manufacturer.builder()
                            .id(UUID.randomUUID())
                            .name("manufacturer name")
                            .address("address")
                            .contact("contact")
                            .build())
                    .categories(List.of(Category.BABY_PRODUCTS))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .reviews(List.of(Review.builder()
                            .reviewerName("Name")
                            .comment("Comment")
                            .rating(5)
                            .reviewDate(LocalDateTime.now())
                            .build()))
                    .build()))
            .customerInfo(CUSTOMER_INFO)
            .deliveryInfo(DELIVERY_INFO)
            .isPaid(false)
            .build();

    private static final Order FINALIZED_ORDER = Order.builder()
            .orderId(UUID.randomUUID())
            .products(List.of(Product.builder()
                    .id(UUID.randomUUID())
                    .name("Test product")
                    .description("Test description")
                    .price(BigDecimal.TEN)
                    .manufacturer(Manufacturer.builder()
                            .id(UUID.randomUUID())
                            .name("manufacturer name")
                            .address("address")
                            .contact("contact")
                            .build())
                    .categories(List.of(Category.BABY_PRODUCTS))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .reviews(List.of(Review.builder()
                            .reviewerName("Name")
                            .comment("Comment")
                            .rating(5)
                            .reviewDate(LocalDateTime.now())
                            .build()))
                    .build()))
            .customerInfo(CUSTOMER_INFO)
            .deliveryInfo(DELIVERY_INFO)
            .isPaid(true)
            .build();

    private static final ShoppingCart SHOPPING_CART = ShoppingCart.builder()
            .id(UUID.randomUUID())
            .products(List.of(Product.builder()
                    .id(UUID.randomUUID())
                    .name("Test product")
                    .description("Test description")
                    .price(BigDecimal.TEN)
                    .manufacturer(Manufacturer.builder()
                            .id(UUID.randomUUID())
                            .name("manufacturer name")
                            .address("address")
                            .contact("contact")
                            .build())
                    .categories(List.of(Category.BABY_PRODUCTS))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .reviews(List.of(Review.builder()
                            .reviewerName("Name")
                            .comment("Comment")
                            .rating(5)
                            .reviewDate(LocalDateTime.now())
                            .build()))
                    .build()))
            .build();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShoppingClient shoppingClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilePart filePart;

    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, shoppingClient, objectMapper);
    }

    @Test
    void should_return_order() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.of(ORDER));

        Order result = orderService.retrieveOrder(orderId);

        assertThat(result).isEqualTo(ORDER);
        verify(orderRepository).findOrderByOrderId(orderId);
    }

    @Test
    void should_not_return_order_if_it_does_not_exist() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.retrieveOrder(orderId));
        verify(orderRepository).findOrderByOrderId(orderId);
    }

    @Test
    void should_create_order() {
        UUID cartId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(shoppingClient.getShoppingCart(cartId)).thenReturn(SHOPPING_CART);
        when(orderRepository.save(any(Order.class))).thenReturn(
                Order.builder()
                        .orderId(orderId)
                        .products(SHOPPING_CART.getProducts())
                        .deliveryInfo(DELIVERY_INFO)
                        .customerInfo(CUSTOMER_INFO)
                        .isPaid(false)
                        .build()
        );

        Order createdOrder = orderService.createOrder(cartId, CUSTOMER_INFO, DELIVERY_INFO);

        assertNotNull(createdOrder.getOrderId());
        assertThat(createdOrder.getProducts()).containsExactlyInAnyOrderElementsOf(SHOPPING_CART.getProducts());
        assertThat(createdOrder.getCustomerInfo()).isEqualTo(CUSTOMER_INFO);
        assertThat(createdOrder.getDeliveryInfo()).isEqualTo(DELIVERY_INFO);
        assertFalse(createdOrder.isPaid());
    }

    @Test
    void should_return_error_if_fetching_cart_returned_error() {
        UUID orderId = ORDER.getOrderId();
        when(shoppingClient.getShoppingCart(orderId)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        assertThrows(ResponseStatusException.class, () -> orderService.createOrder(orderId, CUSTOMER_INFO, DELIVERY_INFO));
    }

    @Test
    void should_delete_order() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.of(ORDER));
        doNothing().when(orderRepository).deleteOrderByOrderId(orderId);

        orderService.deleteOrder(orderId);

        verify(orderRepository).findOrderByOrderId(orderId);
        verify(orderRepository).deleteOrderByOrderId(orderId);
    }

    @Test
    void should_return_error_if_order_not_found() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.deleteOrder(orderId));

        verify(orderRepository).findOrderByOrderId(orderId);
        verify(orderRepository, never()).deleteOrderByOrderId(orderId);
    }

    @Test
    void should_return_error_if_deletion_failed() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.of(ORDER));
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error")).when(orderRepository).deleteOrderByOrderId(orderId);

        assertThrows(ResponseStatusException.class, () -> orderService.deleteOrder(orderId));

        verify(orderRepository).findOrderByOrderId(orderId);
        verify(orderRepository).deleteOrderByOrderId(orderId);
    }

    @Test
    void should_finalize_order() {
        UUID orderId = FINALIZED_ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.of(FINALIZED_ORDER));
        when(orderRepository.save(any(Order.class))).thenReturn(FINALIZED_ORDER);

        Order finalizedOrder = orderService.finalizeOrder(orderId);

        assertTrue(finalizedOrder.isPaid());
        verify(orderRepository).findOrderByOrderId(orderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_return_error_if_order_not_found_for_finalize() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.finalizeOrder(orderId));

        verify(orderRepository).findOrderByOrderId(orderId);
    }

    @Test
    void should_return_error_if_updating_order_failed() {
        UUID orderId = ORDER.getOrderId();
        when(orderRepository.findOrderByOrderId(orderId)).thenReturn(Optional.of(ORDER));
        when(orderRepository.save(any(Order.class))).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error"));

        assertThrows(ResponseStatusException.class, () -> orderService.finalizeOrder(orderId));

        verify(orderRepository).findOrderByOrderId(orderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_upload_orders() throws IOException {
        String jsonContent = "[{\"orderId\":\"...\", ...}]";
        MockMultipartFile file = new MockMultipartFile("file", "orders.json", "application/json", jsonContent.getBytes());

        List<Order> orders = List.of(ORDER, FINALIZED_ORDER);
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(orders);
        when(orderRepository.saveAll(anyList())).thenReturn(orders);

        List<Order> uploadedOrders = orderService.uploadProducts(file);

        assertEquals(2, uploadedOrders.size());
        verify(objectMapper, times(1)).readValue(any(byte[].class), any(TypeReference.class));
        verify(orderRepository, times(1)).saveAll(anyList());
    }

    @Test
    void should_throw_error_when_file_processing_fails() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "orders.json", "application/json", new byte[0]);

        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenThrow(new IOException("Failed to read"));

        assertThrows(ResponseStatusException.class, () -> orderService.uploadProducts(file));
        verify(objectMapper, times(1)).readValue(any(byte[].class), any(TypeReference.class));
        verify(orderRepository, never()).saveAll(anyList());
    }
}