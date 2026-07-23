package com.rajani.makerchecker.web;

import com.rajani.makerchecker.domain.Employee;

public class ViewMapper {

    public static Dtos.EmployeeView toView(Employee e) {
        return new Dtos.EmployeeView(
                e.getId(), e.getName(), e.getTitle(), e.getDepartment(),
                e.getManagerId(), e.roleList(), e.getApprovalLimit());
    }

    private ViewMapper() {}
}
