package com.procureflow;

import com.procureflow.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = TestEnvironmentInitializer.class)
class DomainPersistenceTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    void contextLoadsAndRepositoriesAreInstantiated() {
        assertThat(roleRepository).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(departmentRepository).isNotNull();
        assertThat(categoryRepository).isNotNull();
        assertThat(productRepository).isNotNull();
        assertThat(supplierRepository).isNotNull();
    }
}
