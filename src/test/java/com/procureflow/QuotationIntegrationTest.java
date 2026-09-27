package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.quotation.QuotationItemRequest;
import com.procureflow.dto.quotation.QuotationRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class QuotationIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee;
    private User procurementOfficer;
    private String employeeToken;
    private String procurementToken;
    private Department testDept;
    private Product product1;
    private Product product2;
    private Supplier supplier1;
    private Supplier supplier2;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));

        Role procRole = roleRepository.findByName(RoleName.PROCUREMENT_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.PROCUREMENT_OFFICER).description("Procurement").build()));

        testDept = departmentRepository.findByName("Quotation Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("Quotation Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("Quotation Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Quotation Test Category").active(true).build()));

        product1 = productRepository.findBySku("QT-PROD-1")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("QT-PROD-1")
                        .name("Commercial Printer")
                        .category(category)
                        .unit(ProductUnit.PIECE)
                        .active(true)
                        .build()));

        product2 = productRepository.findBySku("QT-PROD-2")
                .orElseGet(() -> productRepository.save(Product.builder()
                        .sku("QT-PROD-2")
                        .name("Toner Cartridge")
                        .category(category)
                        .unit(ProductUnit.BOX)
                        .active(true)
                        .build()));

        supplier1 = supplierRepository.findBySupplierCode("SUP-QT-001")
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .supplierCode("SUP-QT-001")
                        .companyName("Office Tech Supplies")
                        .email("sales@officetech.com")
                        .active(true)
                        .build()));

        supplier2 = supplierRepository.findBySupplierCode("SUP-QT-002")
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .supplierCode("SUP-QT-002")
                        .companyName("Global Printing Solutions")
                        .email("info@globalprint.com")
                        .active(true)
                        .build()));

        employee = userRepository.findByEmail("emp_qt@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-QT-001")
                        .firstName("Emp")
                        .lastName("Quotation")
                        .email("emp_qt@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(employeeRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        procurementOfficer = userRepository.findByEmail("proc_qt@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-QT-PROC")
                        .firstName("Procurement")
                        .lastName("Officer")
                        .email("proc_qt@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(procRole)
                        .department(testDept)
                        .active(true)
                        .build()));

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        procurementToken = jwtService.generateToken(new ProcureFlowUserDetails(procurementOfficer));
    }

    @Test
    @DisplayName("1, 3, 4. Create quotation for approved PR with multiple items, server calculates totals")
    void testCreateQuotationForApprovedPR() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Office Printers Purchase");

        QuotationItemRequest item1 = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(2)
                .unitPrice(new BigDecimal("45000.00"))
                .build();

        QuotationItemRequest item2 = QuotationItemRequest.builder()
                .productId(product2.getId())
                .quantity(5)
                .unitPrice(new BigDecimal("2000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(approvedPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .validUntil(LocalDate.now().plusDays(30))
                .paymentTerms("Net 30")
                .deliveryDays(7)
                .items(List.of(item1, item2))
                .build();

        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.quotationNumber").value(org.hamcrest.Matchers.startsWith("QT-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(100000.00)) // 2*45000 + 5*2000 = 100000
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("2. Reject quotation for non-approved PR with 400 Bad Request")
    void testRejectQuotationForNonApprovedPR() throws Exception {
        PurchaseRequest draftPR = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-DRAFT-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("Draft PR for Quotation Test")
                .status(RequestStatus.DRAFT)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .unitPrice(new BigDecimal("50000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(draftPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Duplicate supplier quotation for same PR is rejected with 400 Bad Request")
    void testDuplicateSupplierQuotationRejected() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Duplicate Supplier Test PR");

        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(approvedPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        // First creation -> 201 Created
        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation for same supplier & PR -> 400 Bad Request
        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6-9. Submit, Compare, Select quotation; Other quotations for same PR become REJECTED")
    void testQuotationSubmitCompareAndSelectionFlow() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Multi-Supplier Comparison PR");

        // Create Quotation 1 for Supplier 1
        Long q1Id = createAndSubmitQuotation(approvedPR, supplier1, new BigDecimal("40000.00"));
        // Create Quotation 2 for Supplier 2
        Long q2Id = createAndSubmitQuotation(approvedPR, supplier2, new BigDecimal("35000.00"));

        // Compare Quotations
        mockMvc.perform(get("/api/quotations/compare/" + approvedPR.getId())
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseRequestId").value(approvedPR.getId()))
                .andExpect(jsonPath("$.quotations.length()").value(2));

        // Select Quotation 2
        mockMvc.perform(post("/api/quotations/" + q2Id + "/select")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(q2Id))
                .andExpect(jsonPath("$.status").value("SELECTED"));

        // Verify Quotation 1 status is now REJECTED
        mockMvc.perform(get("/api/quotations/" + q1Id)
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("10. Employee cannot manage quotations -> 403 Forbidden")
    void testEmployeeCannotManageQuotations() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Employee Forbidden Test PR");

        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(approvedPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Invalid product or supplier returns 404 Not Found")
    void testInvalidProductOrSupplierReturns404() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Invalid Entity Test PR");

        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(99999L)
                .quantity(1)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(approvedPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("12. Invalid state transition (selecting DRAFT quotation) returns 400 Bad Request")
    void testInvalidStateTransitionReturns400() throws Exception {
        PurchaseRequest approvedPR = createApprovedPR("Invalid State Transition PR");

        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(approvedPR.getId())
                .supplierId(supplier1.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long draftQuotationId = objectMapper.readTree(response).get("id").asLong();

        // Attempting to select DRAFT quotation directly -> 400 Bad Request
        mockMvc.perform(post("/api/quotations/" + draftQuotationId + "/select")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isBadRequest());
    }

    private PurchaseRequest createApprovedPR(String title) {
        return purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-APPROVED-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title(title)
                .status(RequestStatus.APPROVED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());
    }

    private Long createAndSubmitQuotation(PurchaseRequest pr, Supplier supplier, BigDecimal unitPrice) throws Exception {
        QuotationItemRequest item = QuotationItemRequest.builder()
                .productId(product1.getId())
                .quantity(1)
                .unitPrice(unitPrice)
                .build();

        QuotationRequest request = QuotationRequest.builder()
                .purchaseRequestId(pr.getId())
                .supplierId(supplier.getId())
                .quotationDate(LocalDate.now())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/quotations")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long quotationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/quotations/" + quotationId + "/submit")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk());

        return quotationId;
    }
}
