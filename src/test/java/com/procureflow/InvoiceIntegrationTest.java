package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.procureflow.dto.invoice.CreateInvoiceRequest;
import com.procureflow.dto.invoice.InvoiceItemRequest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class InvoiceIntegrationTest {

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
    private GoodsReceiptRepository goodsReceiptRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee;
    private User financeOfficer;
    private String employeeToken;
    private String financeToken;

    private Department testDept;
    private Product product1;
    private Product product2;
    private Supplier supplier1;
    private Supplier supplier2;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));
        Role finRole = roleRepository.findByName(RoleName.FINANCE_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.FINANCE_OFFICER).description("Finance").build()));

        testDept = departmentRepository.findByName("INV Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("INV Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("INV Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("INV Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        product1 = productRepository.save(Product.builder()
                .sku("INV-PROD-1-" + prodSuffix)
                .name("Server Rack " + prodSuffix)
                .category(category)
                .unit(ProductUnit.SET)
                .active(true)
                .build());

        product2 = productRepository.save(Product.builder()
                .sku("INV-PROD-2-" + prodSuffix)
                .name("Network Switch " + prodSuffix)
                .category(category)
                .unit(ProductUnit.PIECE)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier1 = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-INV-1-" + suppSuffix)
                .companyName("Enterprise IT Direct " + suppSuffix)
                .email("sales@enterpriseit-" + suppSuffix + ".com")
                .active(true)
                .build());

        supplier2 = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-INV-2-" + suppSuffix)
                .companyName("Alternative Vendors Ltd " + suppSuffix)
                .email("info@altvendors-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        employee = userRepository.save(User.builder()
                .employeeCode("EMP-INV-" + userSuffix)
                .firstName("Emp")
                .lastName("INV")
                .email("emp_inv_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        financeOfficer = userRepository.save(User.builder()
                .employeeCode("FIN-INV-" + userSuffix)
                .firstName("Finance")
                .lastName("INV")
                .email("fin_inv_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(finRole)
                .department(testDept)
                .active(true)
                .build());

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        financeToken = jwtService.generateToken(new ProcureFlowUserDetails(financeOfficer));
    }

    @Test
    @DisplayName("1, 10. Invoice creation and PostgreSQL persistence")
    void testInvoiceCreationAndPostgresPersistence() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId1 = po.getItems().get(0).getId();
        Long poItemId2 = po.getItems().get(1).getId();

        InvoiceItemRequest item1 = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .quantity(10)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        InvoiceItemRequest item2 = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId2)
                .quantity(5)
                .unitPrice(new BigDecimal("2000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .supplierInvoiceNumber("SINV-" + UUID.randomUUID().toString().substring(0, 6))
                .invoiceDate(LocalDate.now())
                .taxAmount(new BigDecimal("500.00"))
                .items(List.of(item1, item2))
                .build();

        String response = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.invoiceNumber").value(org.hamcrest.Matchers.startsWith("INV-")))
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.subtotal").value(20000.00)) // 10*1000 + 5*2000
                .andExpect(jsonPath("$.totalAmount").value(20500.00))
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(response).get("id").asLong();

        // Verify PostgreSQL persistence
        assertThat(invoiceRepository.findById(invoiceId)).isPresent();
    }

    @Test
    @DisplayName("2. Duplicate supplier invoice number for same PO rejected with 409 Conflict")
    void testDuplicateInvoiceRejection() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId = po.getItems().get(0).getId();

        String supplierInvNum = "SUP-INV-DUP-001";

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(10)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .supplierInvoiceNumber(supplierInvNum)
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        // First creation -> 201 Created
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate creation -> 409 Conflict
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("3. Supplier mismatch rejected with 400 Bad Request")
    void testSupplierMismatchRejection() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(5)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .supplierId(supplier2.getId()) // PO belongs to supplier1!
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4, 7. Quantity mismatch sets status MISMATCH and generates details")
    void testQuantityMismatchThreeWayMatch() throws Exception {
        // Ordered 10, Goods Receipt accepted 6
        PurchaseOrder po = setupReceivedPO(10, 5, 6, 5);
        Long poItemId = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(10) // Invoiced 10, but GR accepted only 6!
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MISMATCH"))
                .andExpect(jsonPath("$.mismatchReason").value(org.hamcrest.Matchers.containsString("exceeds accepted Goods Receipt quantity")));
    }

    @Test
    @DisplayName("5, 7. Price mismatch sets status MISMATCH and generates details")
    void testPriceMismatchThreeWayMatch() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(10)
                .unitPrice(new BigDecimal("1500.00")) // PO unit price is 1000.00!
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MISMATCH"))
                .andExpect(jsonPath("$.mismatchReason").value(org.hamcrest.Matchers.containsString("differs from PO unit price")));
    }

    @Test
    @DisplayName("6, 8. Successful three-way match and approval workflow")
    void testSuccessfulThreeWayMatchAndApproval() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .quantity(10)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(response).get("id").asLong();

        // Get detailed match breakdown endpoint
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/match")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.matchStatus").value("MATCHED"));

        // Approve matched invoice
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("8. Approval fails for mismatched invoice with 400 Bad Request")
    void testApprovalFailsForMismatchedInvoice() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(10)
                .unitPrice(new BigDecimal("1200.00")) // Mismatched price!
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        String response = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MISMATCH"))
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(response).get("id").asLong();

        // Attempt to approve mismatched invoice -> 400 Bad Request
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Employee receives 403 Forbidden when creating invoice")
    void testEmployeeReceives403() throws Exception {
        PurchaseOrder po = setupReceivedPO(10, 5, 10, 5);
        Long poItemId = po.getItems().get(0).getId();

        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .quantity(5)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .purchaseOrderId(po.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private PurchaseOrder setupReceivedPO(int item1OrderQty, int item2OrderQty, int item1AcceptedQty, int item2AcceptedQty) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-INV-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("Invoice Test PR")
                .status(RequestStatus.APPROVED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        Quotation quotation = quotationRepository.save(Quotation.builder()
                .quotationNumber("QT-INV-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier1)
                .quotationDate(LocalDate.now())
                .status(QuotationStatus.SELECTED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-INV-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .quotation(quotation)
                .supplier(supplier1)
                .orderDate(LocalDate.now())
                .status(POStatus.FULLY_RECEIVED)
                .subtotal(new BigDecimal("20000.00"))
                .totalAmount(new BigDecimal("20000.00"))
                .build();

        PurchaseOrderItem poItem1 = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product1)
                .quantity(item1OrderQty)
                .unitPrice(new BigDecimal("1000.00"))
                .totalPrice(new BigDecimal(1000L * item1OrderQty))
                .build();

        PurchaseOrderItem poItem2 = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product2)
                .quantity(item2OrderQty)
                .unitPrice(new BigDecimal("2000.00"))
                .totalPrice(new BigDecimal(2000L * item2OrderQty))
                .build();

        po.addItem(poItem1);
        po.addItem(poItem2);

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Create Goods Receipt
        GoodsReceipt gr = GoodsReceipt.builder()
                .receiptNumber("GR-INV-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseOrder(savedPo)
                .receivedDate(LocalDate.now())
                .receivedBy(employee)
                .status(GoodsReceiptStatus.RECEIVED)
                .build();

        GoodsReceiptItem grItem1 = GoodsReceiptItem.builder()
                .goodsReceipt(gr)
                .purchaseOrderItem(savedPo.getItems().get(0))
                .receivedQuantity(item1AcceptedQty)
                .acceptedQuantity(item1AcceptedQty)
                .rejectedQuantity(0)
                .build();

        GoodsReceiptItem grItem2 = GoodsReceiptItem.builder()
                .goodsReceipt(gr)
                .purchaseOrderItem(savedPo.getItems().get(1))
                .receivedQuantity(item2AcceptedQty)
                .acceptedQuantity(item2AcceptedQty)
                .rejectedQuantity(0)
                .build();

        gr.addItem(grItem1);
        gr.addItem(grItem2);

        goodsReceiptRepository.save(gr);

        return savedPo;
    }
}
