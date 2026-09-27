package com.procureflow.service;

import com.procureflow.dto.approval.ApprovalRuleRequest;
import com.procureflow.dto.approval.ApprovalRuleResponse;
import com.procureflow.entity.ApprovalRule;
import com.procureflow.exception.BadRequestException;
import com.procureflow.exception.ResourceNotFoundException;
import com.procureflow.mapper.ApprovalRuleMapper;
import com.procureflow.repository.ApprovalRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRuleService {

    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApprovalRuleMapper approvalRuleMapper;

    @Transactional
    public ApprovalRuleResponse create(ApprovalRuleRequest request) {
        validateAmounts(request);

        ApprovalRule rule = approvalRuleMapper.toEntity(request);
        ApprovalRule saved = approvalRuleRepository.save(rule);
        log.info("Created approval rule id={} min={} max={} role={}", saved.getId(), saved.getMinimumAmount(), saved.getMaximumAmount(), saved.getRequiredRole());
        return approvalRuleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleResponse> getAll() {
        return approvalRuleRepository.findAll()
                .stream()
                .map(approvalRuleMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalRuleResponse getById(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRule", "id", id));
        return approvalRuleMapper.toResponse(rule);
    }

    @Transactional
    public ApprovalRuleResponse update(Long id, ApprovalRuleRequest request) {
        validateAmounts(request);

        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRule", "id", id));

        approvalRuleMapper.updateEntity(rule, request);
        ApprovalRule updated = approvalRuleRepository.save(rule);
        log.info("Updated approval rule id={} min={} max={}", updated.getId(), updated.getMinimumAmount(), updated.getMaximumAmount());
        return approvalRuleMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRule", "id", id));

        approvalRuleRepository.delete(rule);
        log.info("Deleted approval rule id={}", id);
    }

    private void validateAmounts(ApprovalRuleRequest request) {
        if (request.getMinimumAmount() != null && request.getMaximumAmount() != null) {
            if (request.getMinimumAmount().compareTo(request.getMaximumAmount()) > 0) {
                throw new BadRequestException("Minimum amount must be less than or equal to maximum amount");
            }
        }
    }
}
