package com.rajani.makerchecker.web;

import com.rajani.makerchecker.service.TokenAuth;
import org.springframework.stereotype.Component;

/** Resolves the acting employee id from the bearer token on each request. */
@Component
public class AuthSupport {

    private final TokenAuth tokenAuth;

    public AuthSupport(TokenAuth tokenAuth) {
        this.tokenAuth = tokenAuth;
    }

    public String currentUser(String authorizationHeader) {
        return tokenAuth.requireUser(authorizationHeader);
    }
}
