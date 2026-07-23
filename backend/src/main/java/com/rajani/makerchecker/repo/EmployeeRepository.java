package com.rajani.makerchecker.repo;

import com.rajani.makerchecker.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findAllByOrderByIdAsc();
}
