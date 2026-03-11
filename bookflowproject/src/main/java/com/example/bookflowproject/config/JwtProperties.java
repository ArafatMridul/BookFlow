package com.example.bookflowproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtProperties {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private int expiration;

    public String getSecret() {
        return secret;
    }

    public int getExpiration() {
        return expiration;
    }
}

