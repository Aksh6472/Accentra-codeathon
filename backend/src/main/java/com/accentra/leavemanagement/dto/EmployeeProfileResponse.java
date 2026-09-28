package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.Role;

import java.time.LocalDate;

public record EmployeeProfileResponse(
        Long employeeId,
        Long userId,
        String email,
        String fullName,
        String employeeCode,
        String jobTitle,
        Role role,
        LocalDate joiningDate,
        Long teamId,
        String teamName,
        Long managerId,
        String managerName) {

    public static EmployeeProfileResponse from(Employee employee) {
        Team team = employee.getTeam();
        Employee manager = employee.getManager();
        return new EmployeeProfileResponse(
                employee.getId(),
                employee.getUser().getId(),
                employee.getUser().getEmail(),
                employee.getFullName(),
                employee.getEmployeeCode(),
                employee.getJobTitle(),
                employee.getUser().getRole(),
                employee.getJoiningDate(),
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null);
    }
}
