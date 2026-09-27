package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.purchaserequest.PurchaseRequestItemRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestRequest;
import com.procureflow.entity.*;
import com.procureflow.enums.ProductUnit;
import com.procureflow.enums.RequestStatus;
import com.procureflow.enums.RoleName;
import com.procureflow.repository.*;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class PurchaseRequestIntegrationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee1;
    private User employee2;
    private User manager;
    private String employee1Token;
    private String employee2Token;
    private String managerToken;
    private Department testDept;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.MANAGER).description("Manager").build()));

        testDept = departmentRepository.findByName("Engineering PR Test")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("Engineering PR Test").active(true).build()));

        Category category = categoryRepository.findByName("Electronics PR Test")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Electronics PR Test").active(true).build()));

        product1 = productRepository.findBySku("PR-TEST-SKU-1")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("PR-TEST-SKU-1")
                        .name("Monitor 27-inch")
                        .category(category)
                        .unit(ProductUnit.PIECE)
                        .active(true)
                        .build()));

        product2 = productRepository.findBySku("PR-TEST-SKU-2")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("PR-TEST-SKU-2")
                        .name("Mechanical Keyboard")
                        .category(category)
                        .unit(ProductUnit.SET)
                        .active(true)
                        .build()));

        employee1 = userRepository.findByEmail("emp1_pr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-PR-001")
                        .firstName("Employee")
                        .lastName("One")
                        .email("emp1_pr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(employeeRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        employee2 = userRepository.findByEmail("emp2_pr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-PR-002")
                        .firstName("Employee")
                        .lastName("Two")
                        .email("emp2_pr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(employeeRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        manager = userRepository.findByEmail("mgr_pr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-PR-MGR")
                        .firstName("Manager")
                        .lastName("PR")
                        .email("mgr_pr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(managerRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        employee1Token = jwtService.generateToken(new ProcureFlowUserDetails(employee1));
        employee2Token = jwtService.generateToken(new ProcureFlowUserDetails(employee2));
        managerToken = jwtService.generateToken(new ProcureFlowUserDetails(manager));
    }

    @Test
    @DisplayName("1-3. Employee creates draft PR with multiple items, server calculates totals correctly")
    void testCreateDraftPurchaseRequest() throws Exception {
        PurchaseRequestItemRequest item1 = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(2)
                .estimatedUnitPrice(new BigDecimal("15000.00"))
                .build();

        PurchaseRequestItemRequest item2 = PurchaseRequestItemRequest.builder()
                .productId(product2.getId())
                .quantity(3)
                .estimatedUnitPrice(new BigDecimal("3000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Developer Workstation Setup")
                .reason("Hardware for new joiner")
                .departmentId(testDept.getId())
                .items(List.of(item1, item2))
                .build();

        mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.requestNumber").value(org.hamcrest.Matchers.startsWith("PR-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.requestedByEmail").value("emp1_pr@procureflow.com"))
                .andExpect(jsonPath("$.totalAmount").value(39000.00)) // 2*15000 + 3*3000 = 39000
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("4. Employee can update draft purchase request")
    void testUpdateDraftPurchaseRequest() throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("12000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Initial Title")
                .reason("Initial Reason")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String content = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(content).get("id").asLong();

        // Update draft
        PurchaseRequestItemRequest updatedItem = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(2)
                .estimatedUnitPrice(new BigDecimal("14000.00"))
                .build();

        PurchaseRequestRequest updateRequest = PurchaseRequestRequest.builder()
                .title("Updated Title")
                .reason("Updated Reason")
                .departmentId(testDept.getId())
                .items(List.of(updatedItem))
                .build();

        mockMvc.perform(put("/api/purchase-requests/" + prId)
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.totalAmount").value(28000.00)); // 2 * 14000
    }

    @Test
    @DisplayName("5-6. Employee can submit draft PR and status becomes PENDING_APPROVAL")
    void testSubmitPurchaseRequest() throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("10000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Office Chair")
                .reason("Ergonomic chair request")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String content = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(content).get("id").asLong();

        // Submit PR
        mockMvc.perform(post("/api/purchase-requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("7. Empty PR cannot be created/submitted")
    void testEmptyPurchaseRequestRejected() throws Exception {
        PurchaseRequestRequest emptyRequest = PurchaseRequestRequest.builder()
                .title("Empty PR")
                .reason("No items")
                .departmentId(testDept.getId())
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Invalid product in PR is rejected with 404 Not Found")
    void testInvalidProductInPurchaseRequest() throws Exception {
        PurchaseRequestItemRequest invalidItem = PurchaseRequestItemRequest.builder()
                .productId(99999L)
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("100.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Invalid Product PR")
                .departmentId(testDept.getId())
                .items(List.of(invalidItem))
                .build();

        mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Employee cannot edit another user's PR")
    void testEmployeeCannotEditAnotherUsersPR() throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("5000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Emp1 PR")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String content = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(content).get("id").asLong();

        // Employee 2 attempts to update Employee 1's PR
        mockMvc.perform(put("/api/purchase-requests/" + prId)
                        .header("Authorization", "Bearer " + employee2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Submitted PR cannot be edited")
    void testSubmittedPRCannotBeEdited() throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("5000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("PR to Submit")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String content = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(content).get("id").asLong();

        // Submit PR
        mockMvc.perform(post("/api/purchase-requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        // Attempt to edit submitted PR -> 400 Bad Request
        mockMvc.perform(put("/api/purchase-requests/" + prId)
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/purchase-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. Manager/Admin can view submitted and pending PRs")
    void testManagerCanViewSubmittedRequests() throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(product1.getId())
                .quantity(2)
                .estimatedUnitPrice(new BigDecimal("8000.00"))
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("PR for Manager Review")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String content = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(content).get("id").asLong();

        // Submit PR
        mockMvc.perform(post("/api/purchase-requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + employee1Token))
                .andExpect(status().isOk());

        // Manager views all PRs
        mockMvc.perform(get("/api/purchase-requests")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + prId + ")].status").value("PENDING_APPROVAL"));
    }
}
