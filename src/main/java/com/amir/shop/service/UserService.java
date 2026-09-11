package com.amir.shop.service;

import com.amir.shop.dto.UserRequest;
import com.amir.shop.dto.UserResponse;
import com.amir.shop.entity.User;
import com.amir.shop.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким email уже существует");
        }

        User user = new User(

                null,
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = repository.save(user);

        UserResponse response = new UserResponse(

                savedUser.getUserId(),
                savedUser.getName(),
                savedUser.getEmail()
        );

        return response;
    }

    public UserResponse getUserById(Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail()
        );
    }

}
