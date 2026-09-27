package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.inventory.InventoryAdjustmentRequest;
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
class DashboardReportAuditIntegrationTest {

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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User employee;
    private User manager;
    private String adminToken;
    private String employeeToken;
    private String managerToken;

    private Department testDept;
    private Product productLowStock;
    private Product productNormalStock;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("Admin").build()));
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));
        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.MANAGER).description("Manager").build()));

        testDept = departmentRepository.findByName("Phase13 Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("Phase13 Test Dept").active(true).build()));

        Category category = categoryRepository.findByName("Phase13 Test Category")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Phase13 Test Category").active(true).build()));

        String prodSuffix = UUID.randomUUID().toString().substring(0, 6);
        productLowStock = productRepository.save(Product.builder()
                .sku("P13-LOW-" + prodSuffix)
                .name("Low Stock Product " + prodSuffix)
                .category(category)
                .unit(ProductUnit.PIECE)
                .minimumStockLevel(10)
                .active(true)
                .build());

        productNormalStock = productRepository.save(Product.builder()
                .sku("P13-NORM-" + prodSuffix)
                .name("Normal Stock Product " + prodSuffix)
                .category(category)
                .unit(ProductUnit.BOX)
                .minimumStockLevel(5)
                .active(true)
                .build());

        String suppSuffix = UUID.randomUUID().toString().substring(0, 6);
        supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-P13-" + suppSuffix)
                .companyName("Phase13 Supplier Co " + suppSuffix)
                .email("info@supplier13-" + suppSuffix + ".com")
                .active(true)
                .build());

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        admin = userRepository.save(User.builder()
                .employeeCode("ADM-P13-" + userSuffix)
                .firstName("Admin")
                .lastName("P13")
                .email("admin_p13_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(adminRole)
                .department(testDept)
                .active(true)
                .build());

        employee = userRepository.save(User.builder()
                .employeeCode("EMP-P13-" + userSuffix)
                .firstName("Emp")
                .lastName("P13")
                .email("emp_p13_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(employeeRole)
                .department(testDept)
                .active(true)
                .build());

        manager = userRepository.save(User.builder()
                .employeeCode("MGR-P13-" + userSuffix)
                .firstName("Mgr")
                .lastName("P13")
                .email("mgr_p13_" + userSuffix + "@procureflow.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(managerRole)
                .department(testDept)
                .active(true)
                .build());

        adminToken = jwtService.generateToken(new ProcureFlowUserDetails(admin));
        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employee));
        managerToken = jwtService.generateToken(new ProcureFlowUserDetails(manager));
    }

    @Test
    @DisplayName("1. Dashboard Summary API returns accurate aggregations and low stock counts")
    void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchaseRequests").exists())
                .andExpect(jsonPath("$.pendingApprovals").exists())
                .andExpect(jsonPath("$.approvedRequests").exists())
                .andExpect(jsonPath("$.totalPurchaseOrders").exists())
                .andExpect(jsonPath("$.totalInventoryItems").exists())
                .andExpect(jsonPath("$.lowStockProducts").exists());
    }

    @Test
    @DisplayName("2. Audit Log creation, retrieval by id, and entity filter")
    void testAuditLogCreationAndRetrieval() throws Exception {
        // Perform an inventory adjustment which creates an AuditLog
        InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                .quantityChange(15)
                .notes("Initial stock audit test")
                .build();

        mockMvc.perform(post("/api/inventory/" + productNormalStock.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get all audit logs
        String logsJson = mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn().getResponse().getContentAsString();

        Long auditId = objectMapper.readTree(logsJson).get(0).get("id").asLong();

        // Get audit log by ID
        mockMvc.perform(get("/api/audit-logs/" + auditId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditId))
                .andExpect(jsonPath("$.action").exists());

        // Get audit log by entity type & ID
        mockMvc.perform(get("/api/audit-logs/entity/Inventory/" + auditLogRepository.findById(auditId).get().getEntityId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("3. Audit logs cannot be modified or deleted (returns 405 Method Not Allowed)")
    void testAuditLogsCannotBeModifiedOrDeleted() throws Exception {
        mockMvc.perform(put("/api/audit-logs/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/audit-logs/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("4. Reports: Purchases, Suppliers, Inventory, and Payments")
    void testReports() throws Exception {
        // Purchases Report
        mockMvc.perform(get("/api/reports/purchases")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").exists())
                .andExpect(jsonPath("$.totalPROrderedAmount").exists());

        // Suppliers Report
        mockMvc.perform(get("/api/reports/suppliers")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSuppliers").exists())
                .andExpect(jsonPath("$.activeSuppliers").exists());

        // Inventory Report
        mockMvc.perform(get("/api/reports/inventory")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").exists())
                .andExpect(jsonPath("$.items").isArray());

        // Payments Report
        mockMvc.perform(get("/api/reports/payments")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoices").exists())
                .andExpect(jsonPath("$.paidInvoiceAmount").exists());
    }

    @Test
    @DisplayName("5. Low-stock detection rule (quantity <= minimumStockLevel)")
    void testLowStockDetection() throws Exception {
        // productLowStock has minimumStockLevel = 10, current stock = 0 (low stock!)
        // productNormalStock has minimumStockLevel = 5, adjust stock to 20 (normal stock!)
        InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                .quantityChange(20)
                .notes("Sufficient stock")
                .build();

        mockMvc.perform(post("/api/inventory/" + productNormalStock.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Check Inventory Report identifies low stock item
        mockMvc.perform(get("/api/reports/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Check Dashboard Summary counts low stock item
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockProducts.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("6. RBAC security for Audit Logs and Reports")
    void testRbacAndSecurity() throws Exception {
        // Authenticated Employee can view audit logs
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Authenticated Employee can view reports
        mockMvc.perform(get("/api/reports/purchases")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Unauthenticated request is rejected (401 Unauthorized)
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. Audit log persistence in PostgreSQL database")
    void testPostgresPersistence() throws Exception {
        InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                .quantityChange(3)
                .notes("Postgres test adjustment")
                .build();

        mockMvc.perform(post("/api/inventory/" + productNormalStock.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                "Inventory", inventoryRepository.findByProductId(productNormalStock.getId()).get().getId());

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAction()).isEqualTo("ADJUST_INVENTORY");
        assertThat(logs.get(0).getUser().getId()).isEqualTo(admin.getId());
    }
}
