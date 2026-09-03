package com.example.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    // In-memory catalog store
    private final Map<Long, Product> productCatalog = new ConcurrentHashMap<>() {{
        put(1L, new Product(1L, "Laptop", 899.99, 15));
        put(2L, new Product(2L, "Wireless Mouse", 24.50, 50));
        put(3L, new Product(3L, "Mechanical Keyboard", 75.00, 30));
    }};

    @GetMapping("/products")
    public Collection<Product> getAllProducts() {
        return productCatalog.values();
    }

    @GetMapping("/products/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productCatalog.getOrDefault(id, null);
    }

    @PostMapping("/orders")
    public OrderResponse placeOrder(@RequestBody OrderRequest request) {
        Product product = productCatalog.get(request.productId());
        if (product == null) {
            return new OrderResponse(null, "FAILED", "Product not found");
        }
        if (product.stock() < request.quantity()) {
            return new OrderResponse(null, "FAILED", "Insufficient inventory");
        }

        // Deduct stock
        productCatalog.put(product.id(), new Product(product.id(), product.name(), product.price(), product.stock() - request.quantity()));

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        return new OrderResponse(orderId, "CONFIRMED", "Order processed successfully");
    }

    // Data records
    public record Product(Long id, String name, double price, int stock) {}
    public record OrderRequest(Long productId, int quantity) {}
    public record OrderResponse(String orderId, String status, String message) {}
}
