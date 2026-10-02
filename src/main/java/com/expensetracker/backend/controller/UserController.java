package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        return userRepository.save(user);
    }

    @PostMapping("/login")
    public User loginUser(@RequestBody User loginUser) {

        return userRepository
                .findByUsername(loginUser.getUsername())
                .filter(user ->
                        user.getPassword()
                                .equals(loginUser.getPassword()))
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid username or password"
                        ));
    }
}