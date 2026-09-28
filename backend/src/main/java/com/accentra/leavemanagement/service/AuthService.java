package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.EmployeeProfileResponse;
import com.accentra.leavemanagement.dto.LoginRequest;
import com.accentra.leavemanagement.dto.LoginResponse;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.User;
import com.accentra.leavemanagement.exception.AuthenticationFailedException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.UserRepository;
import com.accentra.leavemanagement.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash of a random value; compared against when the email is unknown. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(request.email().trim());
        // Always run a hash comparison so response time does not reveal whether the email exists.
        String hash = candidate.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        User user = candidate
                .filter(u -> passwordMatches && u.isEnabled())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));

        JwtService.IssuedToken token = jwtService.issue(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), EmployeeProfileResponse.from(employee));
    }
}
