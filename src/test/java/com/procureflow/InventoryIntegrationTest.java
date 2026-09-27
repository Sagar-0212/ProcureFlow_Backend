package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.inventory.InventoryAdjustmentRequest;
import com.procureflow.dto.receipt.CreateGoodsReceiptRequest;
import com.procureflow.dto.receipt.GoodsReceiptItemRequest;
import com.procureflow.entity.*;
import com.procureflow.enums.*;
import com.procureflow.repository.*;
import com.procureflow.security.JwtService;
import com.procureflow.security.ProcureFlowUserDetails;
import com.procureflow.service.InventoryService;
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
class InventoryIntegrationTest {

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryService inventoryService;

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

        testDept = departmentRepository.findByName("INV MGT Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("INV MGT Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("INV MGT Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("INV MGT Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        product1 = productRepository.save(Product.builder()
                .sku("INV-MGT-PROD-1-" + prodSuffix)
                .name("Industrial Sensor " + prodSuffix)
                .category(category)
                .unit(ProductUnit.PIECE)
                .active(true)
                .build());

        product2 = productRepository.save(Product.builder()
                .sku("INV-MGT-PROD-2-" + prodSuffix)
                .name("Control Cable " + prodSuffix)
                .category(category)
                .unit(ProductUnit.METER)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-INV-MGT-" + suppSuffix)
                .companyName("Industrial Electronics Co " + suppSuffix)
                .email("sales@industrialelectronics-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        employee = userRepository.save(User.builder()
                .employeeCode("EMP-INV-MGT-" + userSuffix)
                .firstName("Emp")
                .lastName("INVMGT")
                .email("emp_invmgt_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        procurementOfficer = userRepository.save(User.builder()
                .employeeCode("PROC-INV-MGT-" + userSuffix)
                .firstName("Proc")
                .lastName("INVMGT")
                .email("proc_invmgt_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(procRole)
                .department(testDept)
                .active(true)
                .build());

        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        procurementToken = jwtService.generateToken(new ProcureFlowUserDetails(procurementOfficer));
    }

    @Test
    @DisplayName("1, 2, 4, 9. Accepted quantity increases stock, rejected excluded, inventory transaction created and persisted in PostgreSQL")
    void testAcceptedQuantityIncreasesStockAndRejectedExcluded() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        // Received 8, Accepted 6, Rejected 2
        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(8)
                .acceptedQuantity(6)
                .rejectedQuantity(2)
                .remarks("2 units damaged in transit")
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
                .andExpect(status().isCreated());

        // Verify Inventory quantity is exactly 6 (accepted), not 8 (received)
        mockMvc.perform(get("/api/inventory/" + product1.getId())
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(6));

        // 4. Verify InventoryTransaction created
        mockMvc.perform(get("/api/inventory/" + product1.getId() + "/transactions")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionType").value("RECEIPT"))
                .andExpect(jsonPath("$[0].quantity").value(6))
                .andExpect(jsonPath("$[0].balanceAfter").value(6));

        // 9. Verify PostgreSQL persistence
        assertThat(inventoryRepository.findByProductId(product1.getId())).isPresent();
        assertThat(inventoryRepository.findByProductId(product1.getId()).get().getQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("3. Multiple receipts accumulate stock correctly")
    void testMultipleReceiptsAccumulateStock() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        // First receipt: Accepted 4
        GoodsReceiptItemRequest itemReq1 = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(4)
                .acceptedQuantity(4)
                .rejectedQuantity(0)
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateGoodsReceiptRequest.builder()
                                .purchaseOrderId(po.getId())
                                .receivedDate(LocalDate.now())
                                .items(List.of(itemReq1))
                                .build())))
                .andExpect(status().isCreated());

        // Second receipt: Accepted 5
        GoodsReceiptItemRequest itemReq2 = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateGoodsReceiptRequest.builder()
                                .purchaseOrderId(po.getId())
                                .receivedDate(LocalDate.now())
                                .items(List.of(itemReq2))
                                .build())))
                .andExpect(status().isCreated());

        // Accumulated stock should be 4 + 5 = 9
        mockMvc.perform(get("/api/inventory/" + product1.getId())
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(9));
    }

    @Test
    @DisplayName("5. Duplicate receipt processing does not duplicate stock")
    void testDuplicateReceiptProcessingDoesNotDuplicateStock() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        String response = mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateGoodsReceiptRequest.builder()
                                .purchaseOrderId(po.getId())
                                .receivedDate(LocalDate.now())
                                .items(List.of(itemReq))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long grId = objectMapper.readTree(response).get("id").asLong();
        GoodsReceipt gr = goodsReceiptRepository.findById(grId).orElseThrow();

        // Call inventoryService.processGoodsReceipt directly second time
        inventoryService.processGoodsReceipt(gr);

        // Quantity should still be 5, not 10!
        assertThat(inventoryRepository.findByProductId(product1.getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("6. Cancellation does not incorrectly add stock")
    void testCancellationDoesNotIncorrectlyAddStock() throws Exception {
        PurchaseOrder po = createSentToSupplierPO(10, 5);
        Long poItemId1 = po.getItems().get(0).getId();

        GoodsReceiptItemRequest itemReq = GoodsReceiptItemRequest.builder()
                .purchaseOrderItemId(poItemId1)
                .receivedQuantity(5)
                .acceptedQuantity(5)
                .rejectedQuantity(0)
                .build();

        String response = mockMvc.perform(post("/api/goods-receipts")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateGoodsReceiptRequest.builder()
                                .purchaseOrderId(po.getId())
                                .receivedDate(LocalDate.now())
                                .items(List.of(itemReq))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long grId = objectMapper.readTree(response).get("id").asLong();

        // Cancel Goods Receipt
        mockMvc.perform(post("/api/goods-receipts/" + grId + "/cancel")
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk());

        // Stock remains 5 (does not add stock on cancellation)
        assertThat(inventoryRepository.findByProductId(product1.getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("7. Negative inventory adjustment resulting in negative balance is rejected with 400 Bad Request")
    void testNegativeAdjustmentRejected() throws Exception {
        InventoryAdjustmentRequest request = InventoryAdjustmentRequest.builder()
                .quantityChange(-10) // Initial stock is 0!
                .notes("Deduct non-existent stock")
                .build();

        mockMvc.perform(post("/api/inventory/" + product1.getId() + "/adjust")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Employee receives 403 Forbidden for inventory adjustment")
    void testEmployeeGets403() throws Exception {
        InventoryAdjustmentRequest request = InventoryAdjustmentRequest.builder()
                .quantityChange(5)
                .notes("Employee adjustment attempt")
                .build();

        mockMvc.perform(post("/api/inventory/" + product1.getId() + "/adjust")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private PurchaseOrder createSentToSupplierPO(int item1Qty, int item2Qty) {
        PurchaseRequest pr = purchaseRequestRepository.save(PurchaseRequest.builder()
                .requestNumber("PR-INV-MGT-" + UUID.randomUUID().toString().substring(0, 6))
                .requestedBy(employee)
                .department(testDept)
                .title("Inventory Mgt Test PR")
                .status(RequestStatus.APPROVED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        Quotation quotation = quotationRepository.save(Quotation.builder()
                .quotationNumber("QT-INV-MGT-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .supplier(supplier)
                .quotationDate(LocalDate.now())
                .status(QuotationStatus.SELECTED)
                .totalAmount(new BigDecimal("50000.00"))
                .build());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-INV-MGT-" + UUID.randomUUID().toString().substring(0, 6))
                .purchaseRequest(pr)
                .quotation(quotation)
                .supplier(supplier)
                .orderDate(LocalDate.now())
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
}
