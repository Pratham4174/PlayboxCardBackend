package com.example.playbox.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.playbox.model.PlayBoxUser;
import com.example.playbox.model.TransactionEntity;
import com.example.playbox.repository.PlayBoxUserRepository;
import com.example.playbox.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final PlayBoxUserRepository userRepo;
    private final TransactionRepository txRepo;

    public PlayBoxUser getByCardUid(String cardUid) {
        return userRepo.findByCardUid(cardUid).orElse(null);
    }

    public PlayBoxUser createUser(PlayBoxUser user) {
        user.setCreatedAt(LocalDateTime.now().toString());
        user.setUpdatedAt(LocalDateTime.now().toString());
        user.setBalance(0f);
        return userRepo.save(user);
    }

    /* ---------------- ADD BALANCE ---------------- */

    public PlayBoxUser addBalance(String cardUid, float amount, String adminName) {
        PlayBoxUser user = getByCardUid(cardUid);

        user.setBalance(user.getBalance() + amount);
        user.setUpdatedAt(LocalDateTime.now().toString());
       

        saveTransaction(user, "ADD", amount, adminName, "Balance added");

        return userRepo.save(user);
    }

    /* ---------------- DEDUCT BALANCE ---------------- */

    public PlayBoxUser deductBalance(
            String cardUid,
            float amount,
            String deductor,
            String description
    ) {
        PlayBoxUser user = getByCardUid(cardUid);

        if (user.getBalance() < amount) {
            throw new RuntimeException("Insufficient Balance");
        }

        user.setBalance(user.getBalance() - amount);
        user.setUpdatedAt(LocalDateTime.now().toString());

        saveTransaction(user, "DEDUCT", amount, deductor, description);

        return userRepo.save(user);
    }

    /* ---------------- TRANSACTION SAVE ---------------- */

    private void saveTransaction(
            PlayBoxUser user,
            String type,
            float amount,
            String adminName,
            String description
    ) {
        TransactionEntity tx = new TransactionEntity();

        tx.setUserId(user.getId());
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(user.getBalance());
        tx.setTimestamp(LocalDateTime.now().toString());

        // ✅ NEW FIELDS
        tx.setAdminName(adminName);
        tx.setUserName(user.getName());        // null for ADD
        tx.setDescription(description);  // reason / note

        txRepo.save(tx);
    }

    /* ---------------- ADMIN USE CASES ---------------- */

    public List<PlayBoxUser> getAllUsers() {
        return userRepo.findAll();
    }

    public PlayBoxUser getByPhone(String phone) {
        return userRepo.findByPhone(phone);
    }
}
