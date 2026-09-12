package com.amir.shop.service;

import com.amir.shop.dto.OrderItemRequest;
import com.amir.shop.dto.OrderItemResponse;
import com.amir.shop.dto.OrderRequest;
import com.amir.shop.dto.OrderResponse;
import com.amir.shop.entity.*;
import com.amir.shop.repository.OrderRepository;
import com.amir.shop.repository.ProductRepository;
import com.amir.shop.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            itemResponses.add(new OrderItemResponse(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    item.getPrice()
            ));
        }

        return new OrderResponse(
                order.getId(),
                order.getTotalPrice(),
                itemResponses,
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        Order order = new Order();
        order.setUser(user);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        Map<Integer, Integer> quantitiesByProductId = new TreeMap<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            quantitiesByProductId.merge(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity(),
                    Integer::sum
            );
        }

        for (Map.Entry<Integer, Integer> entry : quantitiesByProductId.entrySet()) {

            Integer productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product currentProduct = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Товар не найден"
                    ));

            if (currentProduct.getStock() < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Недостаточно товара на складе");
        }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            items.add(orderItem);

            orderItem.setProduct(currentProduct);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(currentProduct.getPrice());

            currentProduct.setStock(
                    currentProduct.getStock() - quantity
        );

        totalPrice = totalPrice.add(orderItem.getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity())));

        }
        order.setTotalPrice(totalPrice);
        order.setItems(items);
        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse getOrderById(int id, String email) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Заказ не найден"
                ));

        if (order.getUser() == null ||
                !order.getUser().getEmail().equals(email)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Нет доступа к этому заказу"
            );
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(int id, String email) {

        Map<Integer, Integer> quantitiesByProductId = new TreeMap<>();

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Заказ не найден"
                ));

        if (order.getUser() == null ||
                !order.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Нет доступа к этому заказу"
            );
        }

        if (order.getStatus() != OrderStatus.NEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Отменить можно только новый заказ"
            );
        }

        for (OrderItem item : order.getItems()) {
            quantitiesByProductId.merge(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    Integer :: sum
            );
        }

        for (Map.Entry<Integer, Integer> entry : quantitiesByProductId.entrySet()) {
            Product product = productRepository
                    .findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Товар из заказа не найден"
                    ));
            product.setStock(product.getStock() + entry.getValue());
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public Page<OrderResponse> getUserOrders(String email, Pageable pageable) {
        return orderRepository
                .findByUser_Email(email, pageable)
                .map(this::toResponse);
    }
}
