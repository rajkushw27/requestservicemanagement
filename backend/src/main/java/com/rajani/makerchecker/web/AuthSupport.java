package com.rajani.makerchecker.web;

import com.rajani.makerchecker.service.ApprovalException;
import com.rajani.makerchecker.service.TokenAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the acting employee id on each request. Two callers, two paths:
 *
 * <ul>
 *   <li><b>The demo UI (a human):</b> {@code Authorization: Bearer <token>} from
 *       {@code POST /api/v1/auth/login}. The token maps to exactly one employee — see
 *       {@link TokenAuth}.</li>
 *   <li><b>An integrating system (no human in the loop):</b> {@code X-Api-Key: <key>} plus
 *       {@code X-Acting-As: <employeeId>}. The calling system asserts which employee it is
 *       acting on behalf of; this demo trusts that assertion outright once the key matches.
 *       A real integration would map the caller's own authenticated identity (OAuth client,
 *       mTLS cert, service account) to an employee id server-side instead of trusting a
 *       client-supplied header — see CLAUDE.md Identity section.</li>
 * </ul>
 */
@Component
public class AuthSupport {

    private final TokenAuth tokenAuth;
    private final String integrationApiKey;

    public AuthSupport(TokenAuth tokenAuth,
                        @Value("${makerchecker.integration.api-key}") String integrationApiKey) {
        this.tokenAuth = tokenAuth;
        this.integrationApiKey = integrationApiKey;
    }

    public String currentUser(String authorizationHeader, String apiKeyHeader, String actingAsHeader) {
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            if (!apiKeyHeader.equals(integrationApiKey)) {
                throw ApprovalException.unauthorized("Invalid API key.");
            }
            if (actingAsHeader == null || actingAsHeader.isBlank()) {
                throw ApprovalException.badRequest(
                        "X-Acting-As is required alongside X-Api-Key — tell us which employee this call is on behalf of.");
            }
            return actingAsHeader.trim();
        }
        return tokenAuth.requireUser(authorizationHeader);
    }
}
