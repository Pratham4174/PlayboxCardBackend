package com.example.playbox.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.playbox.model.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByUserId(Long userId);
}