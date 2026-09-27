package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.approval.ApprovalDecisionRequest;
import com.procureflow.dto.approval.ApprovalRuleRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestItemRequest;
import com.procureflow.dto.purchaserequest.PurchaseRequestRequest;
import com.procureflow.entity.*;
import com.procureflow.enums.ProductUnit;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class ApprovalWorkflowIntegrationTest {

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
    private ApprovalRuleRepository approvalRuleRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee;
    private User manager;
    private User admin;
    private String employeeToken;
    private String managerToken;
    private String adminToken;
    private Department testDept;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.MANAGER).description("Manager").build()));

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("Admin").build()));

        testDept = departmentRepository.findByName("Approval Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("Approval Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("Approval Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Approval Test Category").active(true).build()));

        testProduct = productRepository.findBySku("APR-PROD-SKU")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("APR-PROD-SKU")
                        .name("Test Approval Monitor")
                        .category(category)
                        .unit(ProductUnit.PIECE)
                        .active(true)
                        .build()));

        employee = userRepository.findByEmail("emp_apr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-APR-001")
                        .firstName("Emp")
                        .lastName("Approval")
                        .email("emp_apr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(employeeRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        manager = userRepository.findByEmail("mgr_apr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-APR-MGR")
                        .firstName("Manager")
                        .lastName("Approval")
                        .email("mgr_apr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(managerRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        admin = userRepository.findByEmail("admin_apr@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-APR-ADM")
                        .firstName("Admin")
                        .lastName("Approval")
                        .email("admin_apr@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(adminRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        managerToken = jwtService.generateToken(new ProcureFlowUserDetails(manager));
        adminToken = jwtService.generateToken(new ProcureFlowUserDetails(admin));

        // Ensure at least one default active approval rule exists for tests
        if (approvalRuleRepository.findMatchingRule(new BigDecimal("50000.00")).isEmpty()) {
            approvalRuleRepository.save(ApprovalRule.builder()
                    .minimumAmount(BigDecimal.ZERO)
                    .maximumAmount(new BigDecimal("1000000.00"))
                    .requiredRole(RoleName.MANAGER)
                    .active(true)
                    .build());
        }
    }

    @Test
    @DisplayName("1-4. Submitted PR creates pending approval; Manager can view & approve it; PR status becomes APPROVED")
    void testSubmittedPRCreatesPendingApprovalAndManagerApproves() throws Exception {
        Long prId = createAndSubmitPR("Server Equipment Approval Test", 2, new BigDecimal("25000.00"));

        // Verify Pending Approval record exists for Manager
        String pendingResponse = mockMvc.perform(get("/api/approvals/pending")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.purchaseRequestId == " + prId + ")].status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long approvalId = approvalRepository.findByPurchaseRequestId(prId).orElseThrow().getId();

        // Manager approves PR
        ApprovalDecisionRequest decision = ApprovalDecisionRequest.builder()
                .comments("Approved for infrastructure budget")
                .build();

        mockMvc.perform(post("/api/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approverEmail").value("mgr_apr@procureflow.com"));

        // Verify PR status is now APPROVED
        mockMvc.perform(get("/api/purchase-requests/" + prId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("5-6. Manager can reject pending approval with reason; PR status becomes REJECTED")
    void testManagerRejectsApprovalWithReason() throws Exception {
        Long prId = createAndSubmitPR("Monitors Reject Test", 1, new BigDecimal("15000.00"));

        Long approvalId = approvalRepository.findByPurchaseRequestId(prId).orElseThrow().getId();

        ApprovalDecisionRequest decision = ApprovalDecisionRequest.builder()
                .comments("Over department budget allocation")
                .build();

        mockMvc.perform(post("/api/approvals/" + approvalId + "/reject")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.comments").value("Over department budget allocation"));

        // Verify PR status is now REJECTED
        mockMvc.perform(get("/api/purchase-requests/" + prId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("7. Rejection without reason fails with 400 Bad Request")
    void testRejectionWithoutReasonFails() throws Exception {
        Long prId = createAndSubmitPR("Rejection Reason Test", 1, new BigDecimal("10000.00"));
        Long approvalId = approvalRepository.findByPurchaseRequestId(prId).orElseThrow().getId();

        ApprovalDecisionRequest emptyDecision = ApprovalDecisionRequest.builder()
                .comments("")
                .build();

        mockMvc.perform(post("/api/approvals/" + approvalId + "/reject")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyDecision)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Employee cannot approve -> 403 Forbidden")
    void testEmployeeCannotApprove() throws Exception {
        Long prId = createAndSubmitPR("Employee Approve Test", 1, new BigDecimal("5000.00"));
        Long approvalId = approvalRepository.findByPurchaseRequestId(prId).orElseThrow().getId();

        mockMvc.perform(post("/api/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Already processed approval cannot be processed again")
    void testAlreadyProcessedApprovalCannotBeReprocessed() throws Exception {
        Long prId = createAndSubmitPR("Double Approval Test", 1, new BigDecimal("8000.00"));
        Long approvalId = approvalRepository.findByPurchaseRequestId(prId).orElseThrow().getId();

        // Approve first time
        mockMvc.perform(post("/api/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        // Approve second time -> 400 Bad Request
        mockMvc.perform(post("/api/approvals/" + approvalId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. No matching approval rule is handled correctly with 400 Bad Request")
    void testNoMatchingApprovalRuleHandled() throws Exception {
        // Create draft PR for large amount that falls outside any active rule range
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .estimatedUnitPrice(new BigDecimal("99999999.00")) // Extremely large amount
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title("Unmatched Amount PR")
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(response).get("id").asLong();

        // Submitting PR without matching rule -> 400 Bad Request
        mockMvc.perform(post("/api/purchase-requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11-12. ADMIN can manage approval rules; Employee restricted with 403")
    void testApprovalRuleManagementRBAC() throws Exception {
        ApprovalRuleRequest ruleRequest = ApprovalRuleRequest.builder()
                .minimumAmount(new BigDecimal("1000.00"))
                .maximumAmount(new BigDecimal("50000.00"))
                .requiredRole(RoleName.MANAGER)
                .build();

        // Employee attempts to create approval rule -> 403 Forbidden
        mockMvc.perform(post("/api/approval-rules")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isForbidden());

        // Admin creates approval rule -> 201 Created
        String content = mockMvc.perform(post("/api/approval-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.requiredRole").value("MANAGER"))
                .andReturn().getResponse().getContentAsString();

        Long ruleId = objectMapper.readTree(content).get("id").asLong();

        // Admin updates rule
        ApprovalRuleRequest updateRequest = ApprovalRuleRequest.builder()
                .minimumAmount(new BigDecimal("1000.00"))
                .maximumAmount(new BigDecimal("75000.00"))
                .requiredRole(RoleName.MANAGER)
                .build();

        mockMvc.perform(put("/api/approval-rules/" + ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maximumAmount").value(75000.00));

        // Admin deletes rule
        mockMvc.perform(delete("/api/approval-rules/" + ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    private Long createAndSubmitPR(String title, int quantity, BigDecimal unitPrice) throws Exception {
        PurchaseRequestItemRequest item = PurchaseRequestItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(quantity)
                .estimatedUnitPrice(unitPrice)
                .build();

        PurchaseRequestRequest request = PurchaseRequestRequest.builder()
                .title(title)
                .reason("Test Reason " + UUID.randomUUID().toString().substring(0, 5))
                .departmentId(testDept.getId())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/purchase-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long prId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/purchase-requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        return prId;
    }
}
