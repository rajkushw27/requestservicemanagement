package com.rajani.makerchecker.web;

import com.rajani.makerchecker.domain.Employee;
import com.rajani.makerchecker.repo.EmployeeRepository;
import com.rajani.makerchecker.service.ApprovalException;
import com.rajani.makerchecker.service.TokenAuth;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmployeeRepository employees;
    private final TokenAuth tokenAuth;

    public AuthController(EmployeeRepository employees, TokenAuth tokenAuth) {
        this.employees = employees;
        this.tokenAuth = tokenAuth;
    }

    @PostMapping("/login")
    public Dtos.LoginResponse login(@RequestBody Dtos.LoginRequest body) {
        String username = body.username() == null ? "" : body.username().trim();
        Employee employee = employees.findById(username)
                .orElseThrow(() -> ApprovalException.unauthorized("Unknown user name or password."));
        if (!employee.getPasswordHash().equals(body.password())) {
            throw ApprovalException.unauthorized("Unknown user name or password.");
        }
        String token = tokenAuth.issue(employee.getId());
        return new Dtos.LoginResponse(token, ViewMapper.toView(employee));
    }
}
