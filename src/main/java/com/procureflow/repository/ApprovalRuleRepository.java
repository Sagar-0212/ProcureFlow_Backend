package com.procureflow.repository;

import com.procureflow.entity.ApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {

    List<ApprovalRule> findByActiveTrue();

    @Query("SELECT r FROM ApprovalRule r WHERE r.active = true AND :amount >= r.minimumAmount AND :amount <= r.maximumAmount")
    Optional<ApprovalRule> findMatchingRule(@Param("amount") BigDecimal amount);
}
