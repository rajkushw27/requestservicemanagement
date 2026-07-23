package com.rajani.makerchecker.web;

import com.rajani.makerchecker.repo.EmployeeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/people")
public class PeopleController {

    private final EmployeeRepository employees;
    private final AuthSupport auth;

    public PeopleController(EmployeeRepository employees, AuthSupport auth) {
        this.employees = employees;
        this.auth = auth;
    }

    @GetMapping
    public List<Dtos.EmployeeView> list(@RequestHeader("Authorization") String authorization) {
        auth.currentUser(authorization);
        return employees.findAllByOrderByIdAsc().stream().map(ViewMapper::toView).toList();
    }
}
