package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.payment.CreatePaymentRequest;
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
class PaymentIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User employee;
    private User financeOfficer;
    private String employeeToken;
    private String financeToken;

    private Department testDept;
    private Product product;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));
        Role finRole = roleRepository.findByName(RoleName.FINANCE_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.FINANCE_OFFICER).description("Finance").build()));

        testDept = departmentRepository.findByName("PAY Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("PAY Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("PAY Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("PAY Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        product = productRepository.save(Product.builder()
                .sku("PAY-PROD-" + prodSuffix)
                .name("Database License " + prodSuffix)
                .category(category)
                .unit(ProductUnit.SET)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-PAY-" + suppSuffix)
                .companyName("Software Systems Inc " + suppSuffix)
                .email("billing@softwaresystems-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        employee = userRepository.save(User.builder()
                .employeeCode("EMP-PAY-" + userSuffix)
                .firstName("Emp")
                .lastName("PAY")
                .email("emp_pay_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        financeOfficer = userRepository.save(User.builder()
                .employeeCode("FIN-PAY-" + userSuffix)
                .firstName("Finance")
                .lastName("PAY")
                .email("fin_pay_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(finRole)
                .department(testDept)
                .active(true)
                .build());

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        financeToken = jwtService.generateToken(new ProcureFlowUserDetails(financeOfficer));
    }

    @Test
    @DisplayName("1, 5, 7. Successful Payment for approved invoice changes invoice status to PAID and persists in PostgreSQL")
    void testPaymentForApprovedInvoice() throws Exception {
        Invoice approvedInvoice = createInvoiceWithStatus(InvoiceStatus.APPROVED, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(approvedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8))
                .notes("Settlement for Invoice")
                .build();

        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.paymentNumber").value(org.hamcrest.Matchers.startsWith("PAY-")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(response).get("id").asLong();

        // 7. Verify PostgreSQL persistence
        assertThat(paymentRepository.findById(paymentId)).isPresent();

        // 5. Verify Invoice status changed to PAID
        Invoice updatedInvoice = invoiceRepository.findById(approvedInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("2. Payment amount mismatch rejected with 400 Bad Request")
    void testAmountMismatchRejection() throws Exception {
        Invoice approvedInvoice = createInvoiceWithStatus(InvoiceStatus.APPROVED, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(approvedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("10000.00")) // Mismatched amount!
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. Unpaid/mismatched/rejected invoice payment fails with 400 Bad Request")
    void testMismatchedOrUnapprovedInvoicePaymentFails() throws Exception {
        Invoice mismatchedInvoice = createInvoiceWithStatus(InvoiceStatus.MISMATCH, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(mismatchedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.CHEQUE)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Duplicate payment for same invoice prevented with 409 Conflict")
    void testDuplicatePaymentPrevention() throws Exception {
        Invoice approvedInvoice = createInvoiceWithStatus(InvoiceStatus.APPROVED, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(approvedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        // First payment -> 201 Created
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate payment attempt -> 409 Conflict
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Pending payment -> Complete workflow updates invoice to PAID")
    void testPendingToCompleteWorkflow() throws Exception {
        Invoice approvedInvoice = createInvoiceWithStatus(InvoiceStatus.APPROVED, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(approvedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.CHEQUE)
                .status(PaymentStatus.PENDING)
                .build();

        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(response).get("id").asLong();

        // Invoice is still APPROVED
        assertThat(invoiceRepository.findById(approvedInvoice.getId()).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.APPROVED);

        // Complete payment
        mockMvc.perform(post("/api/payments/" + paymentId + "/complete")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Invoice status becomes PAID
        assertThat(invoiceRepository.findById(approvedInvoice.getId()).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("6. Employee receives 403 Forbidden for payment operations")
    void testEmployeeGets403() throws Exception {
        Invoice approvedInvoice = createInvoiceWithStatus(InvoiceStatus.APPROVED, new BigDecimal("15000.00"));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .invoiceId(approvedInvoice.getId())
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Invoice createInvoiceWithStatus(InvoiceStatus status, BigDecimal amount) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-PAY-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("PAY Test PR")
                .status(RequestStatus.APPROVED)
                .totalAmount(amount)
                .build());

        Quotation quotation = quotationRepository.save(Quotation.builder()
                .quotationNumber("QT-PAY-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(LocalDate.now())
                .status(QuotationStatus.SELECTED)
                .totalAmount(amount)
                .build());

        PurchaseOrder po = purchaseOrderRepository.save(PurchaseOrder.builder()
                .poNumber("PO-PAY-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .quotation(quotation)
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .status(POStatus.FULLY_RECEIVED)
                .subtotal(amount)
                .totalAmount(amount)
                .build());

        PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product)
                .quantity(1)
                .unitPrice(amount)
                .totalPrice(amount)
                .build();

        po.addItem(poItem);
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);
        PurchaseOrderItem savedPoItem = savedPo.getItems().get(0);

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-PAY-" + UUID.randomUUID().toString().substring(0, 6))
                .supplierInvoiceNumber("SINV-PAY-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseOrder(savedPo)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(amount)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(amount)
                .status(status)
                .build();

        InvoiceItem invoiceItem = InvoiceItem.builder()
                .invoice(invoice)
                .purchaseOrderItem(savedPoItem)
                .quantity(1)
                .unitPrice(amount)
                .totalPrice(amount)
                .build();

        invoice.addItem(invoiceItem);

        return invoiceRepository.save(invoice);
    }
}
