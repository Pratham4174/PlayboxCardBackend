package com.example.playbox.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.playbox.model.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
    
    // Non-paginated versions (existing)
    List<TransactionEntity> findByUserId(Integer userId);
    List<TransactionEntity> findByAdminName(String adminName);
    List<TransactionEntity> findByTimestampBetween(String start, String end);
    List<TransactionEntity> findByUserIdAndAdminName(Integer userId, String adminName);
    List<TransactionEntity> findByUserIdAndTimestampBetween(Integer userId, String start, String end);
    List<TransactionEntity> findByAdminNameAndTimestampBetween(String adminName, String start, String end);
    List<TransactionEntity> findByUserIdAndAdminNameAndTimestampBetween(
        Integer userId, String adminName, String start, String end);
    
    // Paginated versions (NEW - must add these)
    Page<TransactionEntity> findByUserId(Integer userId, Pageable pageable);
    Page<TransactionEntity> findByAdminName(String adminName, Pageable pageable);
    Page<TransactionEntity> findByTimestampBetween(String start, String end, Pageable pageable);
    Page<TransactionEntity> findByUserIdAndAdminName(Integer userId, String adminName, Pageable pageable);
    Page<TransactionEntity> findByUserIdAndTimestampBetween(
        Integer userId, String start, String end, Pageable pageable);
    Page<TransactionEntity> findByAdminNameAndTimestampBetween(
        String adminName, String start, String end, Pageable pageable);
    Page<TransactionEntity> findByUserIdAndAdminNameAndTimestampBetween(
        Integer userId, String adminName, String start, String end, Pageable pageable);
}