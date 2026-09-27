package com.procureflow.service;

import com.procureflow.dto.auth.LoginRequest;
import com.procureflow.dto.auth.LoginResponse;
import com.procureflow.dto.auth.UserResponse;
import com.procureflow.entity.User;
import com.procureflow.repository.UserRepository;
import com.procureflow.security.JwtService;
import com.procureflow.security.ProcureFlowUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        ProcureFlowUserDetails userDetails = (ProcureFlowUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        String jwtToken = jwtService.generateToken(userDetails);
        UserResponse userResponse = mapToUserResponse(user);

        return LoginResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpirationMs())
                .user(userResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().name() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .active(user.isActive())
                .build();
    }
}
