package com.rajani.makerchecker.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo-only session store: an in-memory token -> employeeId map, gone on restart along
 * with everything else in H2. This is intentionally not a real auth system — see
 * CLAUDE.md Identity section. Replace with a JWT filter for anything real.
 */
@Component
public class TokenAuth {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String employeeId) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, employeeId);
        return token;
    }

    public String requireUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw ApprovalException.unauthorized("Sign in to continue.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String employeeId = tokens.get(token);
        if (employeeId == null) {
            throw ApprovalException.unauthorized("Your session has expired. Sign in again.");
        }
        return employeeId;
    }
}
