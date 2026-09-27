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
        Department execDept = departmentRepository.findByName("Executive")
                .orElseGet(() -> departmentRepository.save(
                        Department.builder()
                                .name("Executive")
                                .description("Executive Management Department")
                                .active(true)
                                .build()
                ));

        Department itDept = departmentRepository.findByName("Information Technology")
                .orElseGet(() -> departmentRepository.save(
                        Department.builder()
                                .name("Information Technology")
                                .description("IT Department")
                                .active(true)
                                .build()
                ));

        Department procDept = departmentRepository.findByName("Procurement")
                .orElseGet(() -> departmentRepository.save(
                        Department.builder()
                                .name("Procurement")
                                .description("Procurement & Sourcing Department")
                                .active(true)
                                .build()
                ));

        Department finDept = departmentRepository.findByName("Finance")
                .orElseGet(() -> departmentRepository.save(
                        Department.builder()
                                .name("Finance")
                                .description("Finance & Accounting Department")
                                .active(true)
                                .build()
                ));

        String pwd = StringUtils.hasText(adminPassword) ? adminPassword : "Admin@123";

        seedUser("EMP-ADMIN-001", "System", "Admin", adminEmail, pwd, RoleName.ADMIN, execDept);
        seedUser("EMP-MGR-001", "Sarah", "Manager", "manager@procureflow.com", "Manager@123", RoleName.MANAGER, itDept);
        seedUser("EMP-PROC-001", "Alex", "Procurement", "procurement@procureflow.com", "Procurement@123", RoleName.PROCUREMENT_OFFICER, procDept);
        seedUser("EMP-FIN-001", "Frank", "Finance", "finance@procureflow.com", "Finance@123", RoleName.FINANCE_OFFICER, finDept);
        seedUser("EMP-EMP-001", "Emma", "Employee", "employee@procureflow.com", "Employee@123", RoleName.EMPLOYEE, itDept);
    }

    private void seedUser(String empCode, String firstName, String lastName, String email, String password, RoleName roleName, Department dept) {
        if (!userRepository.existsByEmail(email)) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException(roleName + " role not found"));

            User user = User.builder()
                    .employeeCode(empCode)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .department(dept)
                    .active(true)
                    .build();

            userRepository.save(user);
            log.info("Seeded initial user: {} ({})", email, roleName);
        }
    }
}
