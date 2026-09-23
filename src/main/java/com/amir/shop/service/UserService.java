package com.amir.shop.service;

import com.amir.shop.dto.request.UserRequest;
import com.amir.shop.dto.response.UserResponse;
import com.amir.shop.entity.Role;
import com.amir.shop.entity.User;
import com.amir.shop.repository.OrderRepository;
import com.amir.shop.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;

    public UserService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            OrderRepository orderRepository
    ) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    public UserResponse register(UserRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким email уже существует");
        }

        User savedUser = repository.save(new User(
                null,
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()))
        );

        return toResponse(savedUser);
    }

    public UserResponse getUserById(Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        return toResponse(user);
    }

    public UserResponse getCurrentUser(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        return toResponse(user);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public UserResponse deleteUserById(Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        if (user.getRole() == Role.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя удалить владельца"
            );
        }

        if (orderRepository.existsByUser_UserId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя удалить пользователя, у которого есть заказы"
            );
        }

        repository.delete(user);

        return toResponse(user);
    }

    public UserResponse updateRole(Integer id, Role newRole) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        if (user.getRole() == Role.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя изменить роль владельца"
            );
        }

        if (user.getRole() == newRole) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Пользователь уже имеет эту роль"
            );
        }
        if (newRole == Role.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя назначить роль владельца"
            );
        }

        user.setRole(newRole);
        repository.save(user);

        return toResponse(user);
    }
}
