package com.accentra.leavemanagement;

import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.entity.User;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.enums.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Small builders for unit tests. */
public final class TestData {

    private TestData() {
    }

    public static Team team(long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    public static Employee employee(long id, String name, Team team, Employee manager) {
        User user = new User();
        user.setId(id + 1000);
        user.setEmail(name.toLowerCase().replace(' ', '.') + "@test.com");
        user.setRole(manager == null ? Role.MANAGER : Role.EMPLOYEE);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setUser(user);
        employee.setFullName(name);
        employee.setEmployeeCode("E" + id);
        employee.setJoiningDate(LocalDate.of(2020, 1, 1));
        employee.setTeam(team);
        employee.setManager(manager);
        return employee;
    }

    public static LeaveType leaveType(long id, String code) {
        LeaveType type = new LeaveType();
        type.setId(id);
        type.setCode(code);
        type.setName(code.charAt(0) + code.substring(1).toLowerCase() + " Leave");
        type.setActive(true);
        return type;
    }

    public static LeavePolicy policy(LeaveType type, String entitlement) {
        LeavePolicy policy = new LeavePolicy();
        policy.setId(type.getId());
        policy.setLeaveType(type);
        policy.setAnnualEntitlement(new BigDecimal(entitlement));
        policy.setProrated(true);
        return policy;
    }

    public static LeaveRequest request(long id, Employee employee, LeaveType type, LocalDate start, LocalDate end,
                                       int days, LeaveStatus status) {
        LeaveRequest request = new LeaveRequest();
        request.setId(id);
        request.setEmployee(employee);
        request.setApprover(employee.getManager());
        request.setLeaveType(type);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setDays(days);
        request.setReason("Test");
        request.setStatus(status);
        request.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        request.setUpdatedAt(request.getCreatedAt());
        return request;
    }

    public static LeaveBalance balance(Employee employee, LeaveType type, int year, String allocated, String used,
                                       String pending) {
        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(type);
        balance.setYear(year);
        balance.setAnnualEntitlement(new BigDecimal(allocated));
        balance.setAllocated(new BigDecimal(allocated));
        balance.setUsed(new BigDecimal(used));
        balance.setPending(new BigDecimal(pending));
        return balance;
    }
}
