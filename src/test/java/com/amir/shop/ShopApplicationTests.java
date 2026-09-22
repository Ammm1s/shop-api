package com.amir.shop;

import com.amir.shop.entity.Role;
import com.amir.shop.entity.User;
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
}
