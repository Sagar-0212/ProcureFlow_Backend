package com.procureflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureflow.dto.category.CategoryRequest;
import com.procureflow.dto.department.DepartmentRequest;
import com.procureflow.dto.product.ProductRequest;
import com.procureflow.dto.supplier.SupplierRequest;
import com.procureflow.entity.Category;
import com.procureflow.entity.Department;
import com.procureflow.entity.Role;
import com.procureflow.entity.User;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class MasterDataIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String employeeToken;
    private String procurementOfficerToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).description("Admin").build()));

        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).description("Employee").build()));

        Role procurementRole = roleRepository.findByName(RoleName.PROCUREMENT_OFFICER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.PROCUREMENT_OFFICER).description("Procurement").build()));

        Department dept = departmentRepository.findByName("MasterData Test Dept")
                .orElseGet(() -> departmentRepository.save(Department.builder().name("MasterData Test Dept").active(true).build()));

        User adminUser = userRepository.findByEmail("admin_md_test@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-MD-ADMIN")
                        .firstName("Admin")
                        .lastName("MD")
                        .email("admin_md_test@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(adminRole)
                        .department(dept)
                        .active(true)
                        .build())
        );

        User employeeUser = userRepository.findByEmail("employee_md_test@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-MD-EMP")
                        .firstName("Employee")
                        .lastName("MD")
                        .email("employee_md_test@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(employeeRole)
                        .department(dept)
                        .active(true)
                        .build())
        );

        User procurementUser = userRepository.findByEmail("proc_md_test@procureflow.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .employeeCode("EMP-MD-PROC")
                        .firstName("Procurement")
                        .lastName("Officer")
                        .email("proc_md_test@procureflow.com")
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(procurementRole)
                        .department(dept)
                        .active(true)
                        .build())
        );

        adminToken = jwtService.generateToken(new ProcureFlowUserDetails(adminUser));
        employeeToken = jwtService.generateToken(new ProcureFlowUserDetails(employeeUser));
        procurementOfficerToken = jwtService.generateToken(new ProcureFlowUserDetails(procurementUser));
    }

    // --- Department Tests ---

    @Test
    @DisplayName("Department CRUD & RBAC: Admin creates department, Employee restricted")
    void testDepartmentLifecycle() throws Exception {
        String deptName = "Logistics Dept " + randomSuffix();

        DepartmentRequest request = DepartmentRequest.builder()
                .name(deptName)
                .description("Supply chain operations")
                .build();

        // Employee attempt -> 403 Forbidden
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Admin attempt -> 201 Created
        String responseContent = mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(deptName))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseContent).get("id").asLong();

        // Duplicate name -> 409 Conflict
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Get by ID (Authenticated Employee can view)
        mockMvc.perform(get("/api/departments/" + createdId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(deptName));

        // Toggle Active
        mockMvc.perform(patch("/api/departments/" + createdId + "/toggle-active")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // --- Category Tests ---

    @Test
    @DisplayName("Category CRUD: Procurement Officer can manage categories")
    void testCategoryLifecycle() throws Exception {
        String catName = "Office Supplies " + randomSuffix();

        CategoryRequest request = CategoryRequest.builder()
                .name(catName)
                .description("General stationery and office tools")
                .build();

        // Procurement Officer creates category -> 201 Created
        String responseContent = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + procurementOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(catName))
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(responseContent).get("id").asLong();

        // Update category name
        String updatedCatName = "Stationery " + randomSuffix();
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name(updatedCatName)
                .description("Updated description")
                .build();

        mockMvc.perform(put("/api/categories/" + categoryId)
                        .header("Authorization", "Bearer " + procurementOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedCatName));
    }

    // --- Product Tests ---

    @Test
    @DisplayName("Product CRUD: Product creation linked to Category and ProductUnit")
    void testProductLifecycle() throws Exception {
        String catName = "Electronics " + randomSuffix();
        Category category = categoryRepository.save(Category.builder()
                .name(catName)
                .description("Tech gear")
                .active(true)
                .build());

        String sku = "SKU-LAP-" + randomSuffix();
        ProductRequest request = ProductRequest.builder()
                .sku(sku)
                .name("Enterprise Laptop 15-inch")
                .description("High-spec developer laptop")
                .categoryId(category.getId())
                .unit(ProductUnit.PIECE)
                .build();

        String responseContent = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.categoryName").value(catName))
                .andExpect(jsonPath("$.unit").value("PIECE"))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(responseContent).get("id").asLong();

        // Get product by ID
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Enterprise Laptop 15-inch"));

        // Non-existent category ID -> 404 Not Found
        ProductRequest invalidCategoryRequest = ProductRequest.builder()
                .sku("SKU-INVALID-" + randomSuffix())
                .name("Invalid Product")
                .categoryId(99999L)
                .unit(ProductUnit.BOX)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCategoryRequest)))
                .andExpect(status().isNotFound());
    }

    // --- Supplier Tests ---

    @Test
    @DisplayName("Supplier CRUD: Supplier creation and unique code validation")
    void testSupplierLifecycle() throws Exception {
        String supplierCode = "SUP-" + randomSuffix();

        SupplierRequest request = SupplierRequest.builder()
                .supplierCode(supplierCode)
                .companyName("Acme Tech Solutions")
                .contactPerson("John Doe")
                .email("contact@acme.com")
                .phone("+1234567890")
                .address("123 Tech Street, Silicon Valley")
                .taxIdentifier("TAX-998877")
                .paymentTerms("Net 30")
                .build();

        String responseContent = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + procurementOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierCode").value(supplierCode))
                .andExpect(jsonPath("$.companyName").value("Acme Tech Solutions"))
                .andReturn().getResponse().getContentAsString();

        Long supplierId = objectMapper.readTree(responseContent).get("id").asLong();

        // Duplicate supplier code -> 409 Conflict
        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + procurementOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // List all suppliers
        mockMvc.perform(get("/api/suppliers")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
