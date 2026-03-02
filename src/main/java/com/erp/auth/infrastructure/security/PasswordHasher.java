package com.erp.auth.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}