package com.example.playbox.service;


import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.playbox.model.Booking;
import com.example.playbox.model.PlayBoxUser;
import com.example.playbox.model.Slot;
import com.example.playbox.model.Sport;
import com.example.playbox.model.TransactionEntity;
import com.example.playbox.repository.BookingRepository;
import com.example.playbox.repository.PlayBoxUserRepository;
import com.example.playbox.repository.SlotRepository;
import com.example.playbox.repository.SportRepository;
import com.example.playbox.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final SportRepository sportRepository;
    private final PlayBoxUserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Booking bookSlot(Integer userId, Long slotId, String paymentMode) {
    
        // 1️⃣ Lock slot row
        Slot slot = slotRepository.findWithLockingById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
    
        if (Boolean.TRUE.equals(slot.getBooked())) {
            throw new RuntimeException("Slot already booked");
        }
    
        // 2️⃣ Get sport directly from slot
        Sport sport = slot.getSport();
    
        Float amount = sport.getPricePerHour();
    
        // 3️⃣ Fetch user
        PlayBoxUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        // 4️⃣ Wallet payment
        if ("WALLET".equalsIgnoreCase(paymentMode)) {
    
            if (user.getBalance() == null || user.getBalance() < amount) {
                throw new RuntimeException("Insufficient balance");
            }
    
            user.setBalance(user.getBalance() - amount);
            userRepository.save(user);
    
            TransactionEntity txn = new TransactionEntity();
            txn.setUserId(userId);
            txn.setUserName(user.getName());
            txn.setType("BOOKING");
            txn.setAmount(amount);
            txn.setBalanceAfter(user.getBalance());
            txn.setAdminName("SYSTEM");
            txn.setDescription(sport.getName() + " Booking");
            txn.setTimestamp(Instant.now());
    
            transactionRepository.save(txn);
        }
    
        // 5️⃣ Mark slot booked
        slot.setBooked(true);
        slotRepository.save(slot);
    
        // 6️⃣ Save booking
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setSportId(sport.getId());
        booking.setSlotId(slotId);
        booking.setAmount(amount);
        booking.setStatus("CONFIRMED");
        booking.setPaymentMode(paymentMode);
        booking.setCreatedAt(Instant.now().toString());
    
        return bookingRepository.save(booking);
    }
    
    
}

