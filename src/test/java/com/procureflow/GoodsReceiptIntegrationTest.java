package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.receipt.CreateGoodsReceiptRequest;
import com.procureflow.dto.receipt.GoodsReceiptItemRequest;
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
class GoodsReceiptIntegrationTest {

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
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));
        Role procRole = roleRepository.findByName(RoleName.PROCUREMENT_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.PROCUREMENT_OFFICER).description("Procurement").build()));

        testDept = departmentRepository.findByName("GR Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("GR Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("GR Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("GR Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        product1 = productRepository.save(Product.builder()
                .sku("GR-PROD-1-" + prodSuffix)
                .name("Laser Scanner " + prodSuffix)
                .category(category)
                .unit(ProductUnit.PIECE)
                .active(true)
                .build());

        product2 = productRepository.save(Product.builder()
                .sku("GR-PROD-2-" + prodSuffix)
                .name("Barcode Label Roll " + prodSuffix)
                .category(category)
                .unit(ProductUnit.BOX)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-GR-" + suppSuffix)
                .companyName("Logistics Gear Co " + suppSuffix)
                .email("sales@logisticsgear-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        employee = userRepository.save(User.builder()
                .employeeCode("EMP-GR-" + userSuffix)
                .firstName("Emp")
                .lastName("GR")
                .email("emp_gr_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        procurementOfficer = userRepository.save(User.builder()
                .employeeCode("PROC-GR-" + userSuffix)
                .firstName("Proc")
                .lastName("GR")
                .email("proc_gr_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(procRole)
                .department(testDept)
                .active(true)
                .build());

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        procurementToken = jwtService.generateToken(new ProcureFlowUserDetails(procurementOfficer));
    }

    @Test
    @DisplayName("1-5, 10. Complete Goods Receipt workflow: Partial delivery -> PARTIALLY_RECEIVED -> Second delivery -> FULLY_RECEIVED & PostgreSQL Persistence")
    void testPartialDeliveryAndFullDeliveryWorkflow() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();
        Long poItemId2 = po.getItems().get(1).getId();

        // 1. First partial receipt: Receive 6 of Item 1 (ordered 10)
        GoodsReceiptItemRequest itemReq1 = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(6)
                .acceptedQuantity(6)
                .rejectedQuantity(0)
                .remarks("First partial shipment")
                .build();

        CreateGoodsReceiptRequest receiptReq1 = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .notes("Batch 1 delivery")
                .items(List.of(itemReq1))
                .build();

        String response1 = mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.receiptNumber").value(org.hamcrest.Matchers.startsWith("GR-")))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn().getResponse().getContentAsString();

        Long grId1 = objectMapper.readTree(response1).get("id").asLong();

        // Verify receipt data persisted in PostgreSQL
        assertThat(goodsReceiptRepository.findById(grId1)).isPresent();

        // Verify PO status changed to PARTIALLY_RECEIVED
        PurchaseOrder updatedPo1 = purchaseOrderRepository.findById(po.getId()).orElseThrow();
        assertThat(updatedPo1.getStatus()).isEqualTo(POStatus.PARTIALLY_RECEIVED);

        // 2. Second receipt: Receive remaining 4 of Item 1 and all 5 of Item 2
        GoodsReceiptItemRequest itemReq2_1 = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(4)
                .acceptedQuantity(4)
                .rejectedQuantity(0)
                .remarks("Remaining Item 1")
                .build();

        GoodsReceiptItemRequest itemReq2_2 = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId2)
                .receivedQuantity(5)
                .acceptedQuantity(4)
                .rejectedQuantity(1)
                .remarks("1 rejected due to damaged box")
                .build();

        CreateGoodsReceiptRequest receiptReq2 = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .notes("Batch 2 final delivery")
                .items(List.of(itemReq2_1, itemReq2_2))
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        // Verify PO status changed to FULLY_RECEIVED
        PurchaseOrder updatedPo2 = purchaseOrderRepository.findById(po.getId()).orElseThrow();
        assertThat(updatedPo2.getStatus()).isEqualTo(POStatus.FULLY_RECEIVED);
    }

    @Test
    @DisplayName("6. Receiving more than ordered quantity fails with 400 Bad Request")
    void testReceivingMoreThanOrderedQuantityFails() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(15) // Ordered only 10
                .acceptedQuantity(15)
                .rejectedQuantity(0)
                .build();

        CreateGoodsReceiptRequest receiptReq = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Invalid PO status (e.g. DRAFT PO) fails receiving with 400 Bad Request")
    void testInvalidPOStatusFails() throws Exception {
        // Create a DRAFT PO
        PurchaseOrder draftPO = createPOWithStatus(POStatus.DRAFT, 10);
        Long poItemId = draftPO.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        CreateGoodsReceiptRequest receiptReq = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(draftPO.getId())
                .receivedDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Accepted + Rejected != Received quantity validation fails with 400 Bad Request")
    void testAcceptedRejectedQuantityValidationFails() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(3) // 3 + 1 = 4 != 5
                .rejectedQuantity(1)
                .build();

        CreateGoodsReceiptRequest receiptReq = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Employee cannot create or receive goods receipt -> 403 Forbidden")
    void testEmployeeCannotCreateOrReceive() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        CreateGoodsReceiptRequest receiptReq = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cancel goods receipt updates PO status back correctly")
    void testCancelGoodsReceiptRecalculatesPOStatus() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        CreateGoodsReceiptRequest receiptReq = CreateGoodsReceiptRequest.builder()
                .purchaseOrderId(po.getId())
                .receivedDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        String response = mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long grId = objectMapper.readTree(response).get("id").asLong();

        // PO is PARTIALLY_RECEIVED
        assertThat(purchaseOrderRepository.findById(po.getId()).orElseThrow().getStatus())
                .isEqualTo(POStatus.PARTIALLY_RECEIVED);

        // Cancel receipt
        mockMvc.perform(post("/api/goods-receipts/" + grId + "/cancel")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // PO status should revert to SENT_TO_SUPPLIER
        assertThat(purchaseOrderRepository.findById(po.getId()).orElseThrow().getStatus())
                .isEqualTo(POStatus.SENT_TO_SUPPLIER);
    }

    private PurchaseOrder createSentToSupplierPO(int item1Qty, int item2Qty) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-GR-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("GR Test PR")
                .status(RequestStatus.APPROVED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        Quotation quotation = quotationRepository.save(Quotation.builder()
                .quotationNumber("QT-GR-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(LocalDate.now())
                .validUntil(LocalDate.now().plusDays(30))
                .paymentTerms("Net 30")
                .deliveryDays(7)
                .status(QuotationStatus.SELECTED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-GR-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .quotation(quotation)
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(7))
                .paymentTerms("Net 30")
                .status(POStatus.SENT_TO_SUPPLIER)
                .subtotal(new BigDecimal("50000.00"))
                .totalAmount(new BigDecimal("50000.00"))
                .build();

        PurchaseOrderItem poItem1 = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product1)
                .quantity(item1Qty)
                .unitPrice(new BigDecimal("3000.00"))
                .totalPrice(new BigDecimal(3000L * item1Qty))
                .build();

        PurchaseOrderItem poItem2 = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product2)
                .quantity(item2Qty)
                .unitPrice(new BigDecimal("4000.00"))
                .totalPrice(new BigDecimal(4000L * item2Qty))
                .build();

        po.addItem(poItem1);
        po.addItem(poItem2);

        return purchaseOrderRepository.save(po);
    }

    private PurchaseOrder createPOWithStatus(POStatus status, int itemQty) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-ST-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("Status Test PR")
                .status(RequestStatus.DRAFT)
                .totalAmount(new BigDecimal("10000.00"))
                .build());

        Quotation quotation = quotationRepository.save(Quotation.builder()
                .quotationNumber("QT-ST-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(LocalDate.now())
                .status(QuotationStatus.DRAFT)
                .totalAmount(new BigDecimal("10000.00"))
                .build());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-ST-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .quotation(quotation)
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .status(status)
                .subtotal(new BigDecimal("10000.00"))
                .totalAmount(new BigDecimal("10000.00"))
                .build();

        PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product1)
                .quantity(itemQty)
                .unitPrice(new BigDecimal("1000.00"))
                .totalPrice(new BigDecimal(1000L * itemQty))
                .build();

        po.addItem(poItem);

        return purchaseOrderRepository.save(po);
    }
}
