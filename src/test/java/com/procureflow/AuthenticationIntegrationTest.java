package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.auth.LoginRequest;
import com.procureflow.entity.Department;
import com.procureflow.entity.Role;
import com.procureflow.entity.User;
import com.procureflow.enums.RoleName;
import com.procureflow.repository.DepartmentRepository;
import com.procureflow.repository.RoleRepository;
import com.procureflow.repository.UserRepository;
import com.procureflow.security.JwtService;
import com.procureflow.security.ProcureFlowUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private User inactiveUser;
    private final String rawPassword = "Password@123";

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));

        Department itDept = departmentRepository.findByName("Information Technology")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("Information Technology").active(true).build()));

        if (!userRepository.existsByEmail("testuser@procureflow.com")) {
            testUser = userRepository.save(User.builder()
                    .employeeCode("EMP-TEST-001")
                    .firstName("Test")
                    .lastName("User")
                    .email("testuser@procureflow.com")
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(employeeRole)
                    .department(itDept)
                    .active(true)
                    .build());
        } else {
            testUser = userRepository.findByEmail("testuser@procureflow.com").orElseThrow();
        }

        if (!userRepository.existsByEmail("inactiveuser@procureflow.com")) {
            inactiveUser = userRepository.save(User.builder()
                    .employeeCode("EMP-INACTIVE-001")
                    .firstName("Inactive")
                    .lastName("User")
                    .email("inactiveuser@procureflow.com")
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(employeeRole)
                    .department(itDept)
                    .active(false)
                    .build());
        }
    }

    @Test
    @DisplayName("1. Password encoder correctly hashes and matches raw passwords")
    void testPasswordEncoder() {
        String encoded = passwordEncoder.encode("SecretPass");
        assertThat(passwordEncoder.matches("SecretPass", encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPass", encoded)).isFalse();
    }

    @Test
    @DisplayName("2. Valid login produces JWT token and safe user details")
    void testValidLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@procureflow.com")
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("testuser@procureflow.com"))
                .andExpect(jsonPath("$.user.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("3. Login with invalid password returns 401 Unauthorized")
    void testInvalidPasswordLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@procureflow.com")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Login with inactive user returns 401 Unauthorized")
    void testInactiveUserLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("inactiveuser@procureflow.com")
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Accessing /api/auth/me without Authorization header returns 401 Unauthorized")
    void testMeEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. Accessing /api/auth/me with valid JWT token returns 200 OK")
    void testMeEndpointWithValidToken() throws Exception {
        ProcureFlowUserDetails userDetails = new ProcureFlowUserDetails(testUser);
        String token = jwtService.generateToken(userDetails);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("testuser@procureflow.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }
}
