package com.amir.shop.config;

import com.amir.shop.entity.Role;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter)
        throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
        );

        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/users")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.GET, "/users/*")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.DELETE, "/users/*")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.PATCH, "/users/*/role")
                        .hasRole(Role.OWNER.name())
                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/orders/admin")
                .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.POST, "/products")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.POST, "/categories")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.GET, "/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/orders/*/cancel").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/orders/*/status")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.DELETE, "/products/*")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.PUT, "/products/*")
                        .hasAnyRole(Role.ADMIN.name(), Role.OWNER.name())
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .anyRequest().denyAll()
        );

        http.exceptionHandling(exception -> exception

                .authenticationEntryPoint((
                        request,
                        response,
                        authException
                ) -> {

                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write("""
                    {
                      "status": 401,
                      "message": "Требуется авторизация",
                      "errors": {}
                    }
                    """);
                })
                .accessDeniedHandler((
                        request,
                        response,
                        accessDeniedException
                ) -> {

                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write("""
                    {
                      "status": 403,
                      "message": "Недостаточно прав",
                      "errors": {}
                    }
                    """);
                })
        );

        return http.build();
    }
}
