package com.example.weborderservice.api;

import com.example.weborderservice.model.*;
import com.example.weborderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {

    private static final CustomerInfo CUSTOMER_INFO = CustomerInfo.builder()
            .firstName("Joe")
            .lastName("Doe")
            .email("joedoe@test.com")
            .phoneNumber("555666777")
            .build();

    private static final CustomerInfo INVALID_CUSTOMER_INFO = CustomerInfo.builder()
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

    private static final DeliveryInfo INVALID_DELIVERY_INFO = DeliveryInfo.builder()
            .address("Street 1")
            .city("London")
            .postalCode("33333")
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
    private static final Mono<Order> ERROR = Mono.error(new RuntimeException("Some error"));
    private static final String NOT_UUID_STRING = "not-uuid-string";
    public static final PaymentRequest PAYMENT_REQUEST = new PaymentRequest("41111111111111111", "06", "25", "123", "Joe Doe");
    public static final PaymentRequest INVALID_PAYMENT_REQUEST = new PaymentRequest(null, "06", "25", "123", "Joe Doe");

    public static ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @BeforeAll
    public static void setUpObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void should_return_created_order() throws Exception {
        when(orderService.createOrder(any(), any(), any())).thenReturn(ORDER);

        String createOrderRequestBodyJson = objectMapper.writeValueAsString(new CreateOrderRequestBody(CUSTOMER_INFO, DELIVERY_INFO));

        mockMvc.perform(post("/order/{orderId}", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequestBodyJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(ORDER.getOrderId().toString()));
    }

    @Test
    void should_return_error_for_wrong_url() throws Exception {
        mockMvc.perform(post("/order/{orderId}/test", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequestBody(CUSTOMER_INFO, DELIVERY_INFO))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void should_return_bad_request_for_invalid_customer_info() throws Exception {
        mockMvc.perform(post("/order/{orderId}", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequestBody(INVALID_CUSTOMER_INFO, DELIVERY_INFO))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_for_invalid_delivery_info() throws Exception {
        mockMvc.perform(post("/order/{orderId}", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequestBody(CUSTOMER_INFO, INVALID_DELIVERY_INFO))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_for_invalid_orderId_format() throws Exception {
        mockMvc.perform(post("/order/{orderId}", NOT_UUID_STRING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequestBody(CUSTOMER_INFO, DELIVERY_INFO))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_error_in_case_service_returned_error() throws Exception {
        when(orderService.createOrder(any(), any(), any())).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error"));

        mockMvc.perform(post("/order/{orderId}", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequestBody(CUSTOMER_INFO, DELIVERY_INFO))))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void should_return_order() throws Exception {
        when(orderService.retrieveOrder(any())).thenReturn(ORDER);

        mockMvc.perform(get("/order/{orderId}", ORDER.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(ORDER.getOrderId().toString()));
    }

    @Test
    void should_return_bad_request_for_invalid_orderId_format_for_retrieve() throws Exception {
        mockMvc.perform(get("/order/{orderId}", NOT_UUID_STRING))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_error_in_case_retrieve_error_returned_one() throws Exception {
        when(orderService.retrieveOrder(any())).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error"));

        mockMvc.perform(get("/order/{orderId}", ORDER.getOrderId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void should_return_empty_for_deleted_order() throws Exception {
        doNothing().when(orderService).deleteOrder(any());

        mockMvc.perform(delete("/order/{orderId}", ORDER.getOrderId()))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_bad_request_for_invalid_orderId_format_for_deletion() throws Exception {
        mockMvc.perform(delete("/order/{orderId}", NOT_UUID_STRING))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_error_in_case_service_deletion_returned_error() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error")).when(orderService).deleteOrder(any());

        mockMvc.perform(delete("/order/{orderId}", ORDER.getOrderId()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void should_return_finalized_order() throws Exception {
        when(orderService.finalizeOrder(any())).thenReturn(ORDER);

        String paymentRequestJson = objectMapper.writeValueAsString(PAYMENT_REQUEST);

        mockMvc.perform(post("/order/{orderId}/finalize", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(ORDER.getOrderId().toString()));
    }

    @Test
    void should_return_bad_request_for_invalid_request_body() throws Exception {
        String invalidPaymentRequestJson = objectMapper.writeValueAsString(INVALID_PAYMENT_REQUEST);

        mockMvc.perform(post("/order/{orderId}/finalize", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPaymentRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_for_invalid_orderId_for_finalize() throws Exception {
        String paymentRequestJson = objectMapper.writeValueAsString(PAYMENT_REQUEST);

        mockMvc.perform(post("/order/{orderId}/finalize", NOT_UUID_STRING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_error_if_finalize_returned_error() throws Exception {
        when(orderService.finalizeOrder(any())).thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error"));

        String paymentRequestJson = objectMapper.writeValueAsString(PAYMENT_REQUEST);

        mockMvc.perform(post("/order/{orderId}/finalize", ORDER.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequestJson))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void should_return_success_for_orders_upload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "orders.json", "application/json", "<<json data>>".getBytes());

        when(orderService.uploadProducts(any())).thenReturn(List.of(ORDER));

        mockMvc.perform(multipart("/order/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].orderId").value(ORDER.getOrderId().toString()));
    }

}