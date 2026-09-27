package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.po.CreatePurchaseOrderRequest;
import com.procureflow.entity.*;
import com.procureflow.enums.*;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class PurchaseOrderIntegrationTest {

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
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee;
    private User procurementOfficer;
    private User manager;
    private User admin;

    private String employeeToken;
    private String procurementToken;
    private String managerToken;
    private String adminToken;

    private Department testDept;
    private Product product1;
    private Product product2;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));
        Role procRole = roleRepository.findByName(RoleName.PROCUREMENT_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.PROCUREMENT_OFFICER).description("Procurement").build()));
        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.MANAGER).description("Manager").build()));
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("Admin").build()));

        testDept = departmentRepository.findByName("PO Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("PO Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("PO Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("PO Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        product1 = productRepository.save(Product.builder()
                .sku("PO-PROD-1-" + prodSuffix)
                .name("Workstation PC " + prodSuffix)
                .category(category)
                .unit(ProductUnit.SET)
                .active(true)
                .build());

        product2 = productRepository.save(Product.builder()
                .sku("PO-PROD-2-" + prodSuffix)
                .name("UltraWide Monitor " + prodSuffix)
                .category(category)
                .unit(ProductUnit.PIECE)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-PO-" + suppSuffix)
                .companyName("Tech Hardware Ltd " + suppSuffix)
                .email("sales@techhardware-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        employee = userRepository.save(User.builder()
                .employeeCode("EMP-PO-" + userSuffix)
                .firstName("Emp")
                .lastName("PO")
                .email("emp_po_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        procurementOfficer = userRepository.save(User.builder()
                .employeeCode("PROC-PO-" + userSuffix)
                .firstName("Proc")
                .lastName("PO")
                .email("proc_po_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(procRole)
                .department(testDept)
                .active(true)
                .build());

        manager = userRepository.save(User.builder()
                .employeeCode("MGR-PO-" + userSuffix)
                .firstName("Mgr")
                .lastName("PO")
                .email("mgr_po_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(managerRole)
                .department(testDept)
                .active(true)
                .build());

        admin = userRepository.save(User.builder()
                .employeeCode("ADM-PO-" + userSuffix)
                .firstName("Admin")
                .lastName("PO")
                .email("adm_po_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(adminRole)
                .department(testDept)
                .active(true)
                .build());

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        procurementToken = jwtService.generateToken(new ProcureFlowUserDetails(procurementOfficer));
        managerToken = jwtService.generateToken(new ProcureFlowUserDetails(manager));
        adminToken = jwtService.generateToken(new ProcureFlowUserDetails(admin));
    }

    @Test
    @DisplayName("1, 3, 4, 5. Create PO from selected quotation copies supplier, items and calculates totals")
    void testCreatePOFromSelectedQuotation() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("Selected Quotation Test PR");

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .quotationId(selectedQuotation.getId())
                .paymentTerms("Net 30")
                .notes("Standard Delivery")
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.poNumber").value(org.hamcrest.Matchers.startsWith("PO-")))
                .andExpect(jsonPath("$.supplierId").value(supplier.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(130000.00)); // 1*100000 + 1*30000
    }

    @Test
    @DisplayName("2. Reject PO creation from non-selected quotation with 400 Bad Request")
    void testRejectPOCreationFromNonSelectedQuotation() throws Exception {
        Quotation submittedQuotation = createSubmittedQuotation("Submitted Quotation Test PR");

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .quotationId(submittedQuotation.getId())
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Duplicate PO for same selected quotation is prevented with 409 Conflict")
    void testDuplicatePOPrevented() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("Duplicate PO Test PR");

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .quotationId(selectedQuotation.getId())
                .build();

        // First PO creation -> 201 Created
        mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate creation attempt -> 409 Conflict
        mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("7. Update DRAFT PO succeeds")
    void testUpdateDraftPO() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("Draft Update Test PR");
        Long poId = createDraftPO(selectedQuotation.getId());

        CreatePurchaseOrderRequest updateRequest = CreatePurchaseOrderRequest.builder()
                .quotationId(selectedQuotation.getId())
                .paymentTerms("Net 60")
                .notes("Updated Delivery Notes")
                .expectedDeliveryDate(LocalDate.now().plusDays(20))
                .build();

        mockMvc.perform(put("/api/purchase-orders/" + poId)
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTerms").value("Net 60"))
                .andExpect(jsonPath("$.notes").value("Updated Delivery Notes"));
    }

    @Test
    @DisplayName("8, 9, 10. Complete PO Workflow: Submit -> Approve -> Send")
    void testPOWorkflowSubmitApproveSend() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("Workflow Test PR");
        Long poId = createDraftPO(selectedQuotation.getId());

        // 8. Submit DRAFT -> PENDING_APPROVAL
        mockMvc.perform(post("/api/purchase-orders/" + poId + "/submit")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        // 9. Manager Approval PENDING_APPROVAL -> APPROVED
        mockMvc.perform(post("/api/purchase-orders/" + poId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 10. Send to Supplier APPROVED -> SENT_TO_SUPPLIER
        mockMvc.perform(post("/api/purchase-orders/" + poId + "/send")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT_TO_SUPPLIER"));
    }

    @Test
    @DisplayName("11. Invalid state transitions rejected with 400 Bad Request")
    void testInvalidStateTransitionsRejected() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("Invalid State Test PR");
        Long poId = createDraftPO(selectedQuotation.getId());

        // Attempting to approve DRAFT PO directly -> 400 Bad Request
        mockMvc.perform(post("/api/purchase-orders/" + poId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());

        // Attempting to send DRAFT PO directly -> 400 Bad Request
        mockMvc.perform(post("/api/purchase-orders/" + poId + "/send")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. Employee cannot manage PO -> 403 Forbidden")
    void testEmployeeCannotManagePO() throws Exception {
        Quotation selectedQuotation = createAndSelectQuotation("RBAC Test PR");

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .quotationId(selectedQuotation.getId())
                .build();

        // Employee create -> 403
        mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Quotation createSubmittedQuotation(String prTitle) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-PO-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title(prTitle)
                .status(RequestStatus.APPROVED)
                .totalAmount(new BigDecimal("130000.00"))
                .build());

        Quotation quotation = Quotation.builder()
                .quotationNumber("QT-PO-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(LocalDate.now())
                .validUntil(LocalDate.now().plusDays(30))
                .paymentTerms("Net 30")
                .deliveryDays(10)
                .status(QuotationStatus.SUBMITTED)
                .totalAmount(new BigDecimal("130000.00"))
                .build();

        QuotationItem item1 = QuotationItem.builder()
                .product(product1)
                .quantity(1)
                .unitPrice(new BigDecimal("100000.00"))
                .totalPrice(new BigDecimal("100000.00"))
                .build();

        QuotationItem item2 = QuotationItem.builder()
                .product(product2)
                .quantity(1)
                .unitPrice(new BigDecimal("30000.00"))
                .totalPrice(new BigDecimal("30000.00"))
                .build();

        quotation.addItem(item1);
        quotation.addItem(item2);

        return quotationRepository.save(quotation);
    }

    private Quotation createAndSelectQuotation(String prTitle) {
        Quotation quotation = createSubmittedQuotation(prTitle);
        quotation.setStatus(QuotationStatus.SELECTED);
        return quotationRepository.save(quotation);
    }

    private Long createDraftPO(Long quotationId) throws Exception {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .quotationId(quotationId)
                .build();

        String response = mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
