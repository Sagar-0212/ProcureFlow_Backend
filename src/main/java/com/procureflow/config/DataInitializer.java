package com.procureflow.config;

import com.procureflow.entity.Department;
import com.procureflow.entity.Role;
import com.procureflow.entity.User;
import com.procureflow.enums.RoleName;
import com.procureflow.repository.DepartmentRepository;
import com.procureflow.repository.RoleRepository;
import com.procureflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.procureflow.entity.ApprovalRule;
import com.procureflow.repository.ApprovalRuleRepository;

import java.math.BigDecimal;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${DEFAULT_ADMIN_EMAIL:admin@procureflow.com}")
    private String adminEmail;

    @Value("${DEFAULT_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedRoles();
        seedDefaultDepartmentAndAdmin();
        seedDefaultApprovalRule();
    }

    private void seedDefaultApprovalRule() {
        if (approvalRuleRepository.count() == 0) {
            ApprovalRule defaultRule = ApprovalRule.builder()
                    .minimumAmount(BigDecimal.ZERO)
                    .maximumAmount(new BigDecimal("10000000.00"))
                    .requiredRole(RoleName.MANAGER)
                    .active(true)
                    .build();
            approvalRuleRepository.save(defaultRule);
            log.info("Seeded default approval rule: 0 - 10,000,000 for MANAGER");
        }
    }

    private void seedRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description("System role for " + roleName.name())
                        .build();
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        });
    }

    private void seedDefaultDepartmentAndAdmin() {
        Department adminDept = departmentRepository.findByName("Executive")
                .orElseGet(() -> departmentRepository.save(
                        Department.builder()
                                .name("Executive")
                                .description("Executive Management Department")
                                .active(true)
                                .build()
                ));

        if (!userRepository.existsByEmail(adminEmail)) {
            if (!StringUtils.hasText(adminPassword)) {
                log.warn("DEFAULT_ADMIN_PASSWORD is not configured in local environment. Skipping initial admin user creation.");
                return;
            }

            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

            User adminUser = User.builder()
                    .employeeCode("EMP-ADMIN-001")
                    .firstName("System")
                    .lastName("Administrator")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(adminRole)
                    .department(adminDept)
                    .active(true)
                    .build();

            userRepository.save(adminUser);
            log.info("Seeded initial admin user: {}", adminEmail);
        }
    }
}
