package com.example.weborderservice.client;

import com.example.weborderservice.model.ShoppingCart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ShoppingClient {

    private final RestTemplate restTemplate;
    private final ShoppingConfig config;

    @Autowired
    public ShoppingClient(RestTemplate restTemplate, ShoppingConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public ShoppingCart getShoppingCart(final UUID cartId) {
        try {
            return restTemplate.getForObject(config.getUrl() + "/" + cartId, ShoppingCart.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shopping cart not found", e);
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", e);
        }
    }
}
