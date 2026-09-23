package com.amir.shop;

import com.amir.shop.entity.Order;
import com.amir.shop.entity.OrderStatus;
import com.amir.shop.entity.Role;
import com.amir.shop.entity.User;
import com.amir.shop.repository.OrderRepository;
import com.amir.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest(properties = {
        "shop.jwt.secret=test-only-secret-key-at-least-32-bytes-long"
})
class ShopApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Test
    void contextLoads(){
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "missing@example.com",
                              "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Неверный email или пароль"));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Login Test",
                              "email": "login-test@example.com",
                              "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "login-test@example.com",
                              "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "password",
                              "email": "wrong-password@example.com",
                              "password": "password123"
                            }
                            """)
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "incorrect123"
                                }
                                """)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Неверный email или пароль"));
     }

    @Test
    void getCurrentUserWithoutTokenReturnsUnauthorized() throws Exception {

        mockMvc.perform(
                get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                """)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Требуется авторизация"));
    }

    @Test
    void ownerCanAssignAdminRole() throws Exception {

        User user = new User(

                null,
                "Rest Test",
                "role-test@example.com",
                "unused-in-this-test"
        );

        user = userRepository.save(user);

        mockMvc.perform(
                patch("/users/{id}/role", user.getUserId())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OWNER")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "role": "ADMIN"
                        }
                        """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User updatedUser = userRepository.findById(user.getUserId())
                .orElseThrow();

        assertEquals(Role.ADMIN, updatedUser.getRole());
    }

    @Test
    void adminCannotAssignAdminRole() throws Exception {

        User user = new User(

                null,
                "Admin Test",
                "admin-role-test@example.com",
                "unused-in-this-test"
        );

        user = userRepository.save(user);

        mockMvc.perform(
                patch("/users/{id}/role", user.getUserId())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "role": "ADMIN"
                        }
                        """)
        )
                .andExpect(status().isForbidden());

        User updatedUser = userRepository.findById(user.getUserId())
                .orElseThrow();

        assertEquals(Role.USER, updatedUser.getRole());
    }

    @Test
    void ownerCannotChangeOwnerRole() throws Exception {

        User user = new User(

                null,
                "protected-owner",
                "protected-owner@example.com",
                "unused-in-this-test"
        );

        user.setRole(Role.OWNER);
        user = userRepository.save(user);

        mockMvc.perform(
                patch("/users/{id}/role", user.getUserId())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OWNER")
        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {
                          "role": "ADMIN"
                        }
                        """
                        )
        )
                .andExpect(status().isConflict());

        User updatedUser = userRepository.findById(user.getUserId())
                .orElseThrow();

        assertEquals(Role.OWNER, updatedUser.getRole());
    }

    @Test
    void userCannotUpdateOrderStatus() throws Exception {

        mockMvc.perform(
                patch("/orders/{id}/status", 999)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER")
        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "status": "PAID"
                        }
                        """)
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanMarkNewOrderPaid() throws Exception {

        Order order = new Order(

                BigDecimal.ZERO,
                new ArrayList<>()
        );

        order = orderRepository.save(order);

        mockMvc.perform(
                patch("/orders/{id}/status", order.getId())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "status": "PAID"
                        }
                        """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        Order updatedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
    }

    @Test
    void adminCanShipPaidOrder() throws Exception {

        Order order = new Order(

                BigDecimal.ZERO,
                new ArrayList<>()
        );

        order.setStatus(OrderStatus.PAID);
        order = orderRepository.save(order);

        mockMvc.perform(
                        patch("/orders/{id}/status", order.getId())
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "status": "SHIPPED"
                        }
                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        Order updatedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertEquals(OrderStatus.SHIPPED, updatedOrder.getStatus());
    }

    @Test
    void adminCannotShipNewOrder() throws Exception {

        Order order = new Order(

                BigDecimal.ZERO,
                new ArrayList<>()
        );

        order = orderRepository.save(order);

        mockMvc.perform(
                patch("/orders/{id}/status", order.getId())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "status": "SHIPPED"
                        }
                        """)
        )
                .andExpect(status().isConflict());

        Order updatedOrder = orderRepository.findById(order.getId())
                .orElseThrow();
        assertEquals(OrderStatus.NEW, updatedOrder.getStatus());
    }

    @Test
    void userCanViewOwnOrdersButNotAllOrders() throws Exception {

        mockMvc.perform(
                get("/orders/admin")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER")
                        ))
        )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/orders")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                ))
                )
                .andExpect(status().isOk());
    }

    @Test
    void adminAndOwnerCanViewAllOrders() throws Exception {

        mockMvc.perform(
                        get("/orders/admin")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/orders/admin")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_OWNER")
                                ))
                )
                .andExpect(status().isOk());
    }
}
