package com.example.playbox.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.playbox.dto.RechargeHistoryDTO;
import com.example.playbox.dto.TransactionDTO;
import com.example.playbox.dto.UserDetailsDTO;
import com.example.playbox.dto.UserStatsDTO;
import com.example.playbox.dto.UserSummaryDTO;
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
        tx.setTimestamp(Instant.now());

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

    public List<UserSummaryDTO> getAllUsersSummary() {
        List<PlayBoxUser> users = userRepo.findAll();
        
        return users.stream().map(user -> {
            UserSummaryDTO dto = new UserSummaryDTO();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            
            try {
                dto.setRegistrationDate(
                    LocalDateTime.parse(user.getCreatedAt())
                );
            } catch (Exception e) {
                dto.setRegistrationDate(null);
            }
            
            dto.setCurrentBalance(user.getBalance());
            
            // Calculate stats
            Double totalRecharge = txRepo.getTotalRecharge(user.getId());
            Double totalDeduction = txRepo.getTotalDeduction(user.getId());
            Long totalVisits = txRepo.getTotalVisits(user.getId());
            Instant lastVisit = txRepo.getLastVisit(user.getId()); // Now returns Instant
            
            dto.setTotalRecharge(totalRecharge != null ? totalRecharge : 0.0);
            dto.setTotalDeduction(totalDeduction != null ? totalDeduction : 0.0);
            dto.setTotalVisits(totalVisits != null ? totalVisits.intValue() : 0);
            dto.setLastVisit(lastVisit != null ? lastVisit.toString() : null); // Convert to String
            
            // Determine status (active if visited in last 30 days)
            String status = determineUserStatus(lastVisit);
            dto.setStatus(status);
            
            return dto;
        }).collect(Collectors.toList());
    }
    private String determineUserStatus(Instant lastVisit) {
        if (lastVisit == null) {
            return "inactive";
        }
        
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        return lastVisit.isAfter(thirtyDaysAgo) ? "active" : "inactive";
    }

    public UserDetailsDTO getUserDetails(Integer userId) {
        PlayBoxUser user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserDetailsDTO dto = new UserDetailsDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRegistrationDate(user.getCreatedAt());
        dto.setCurrentBalance(user.getBalance());
        
        // Calculate stats
        Double totalRecharge = txRepo.getTotalRecharge(userId);
        Double totalDeduction = txRepo.getTotalDeduction(userId);
        Long totalVisits = txRepo.getTotalVisits(userId);
        Instant lastVisit = txRepo.getLastVisit(userId); 
        
        dto.setTotalRecharge(totalRecharge != null ? totalRecharge : 0.0);
        dto.setTotalDeduction(totalDeduction != null ? totalDeduction : 0.0);
        dto.setTotalVisits(totalVisits != null ? totalVisits.intValue() : 0);
        dto.setLastVisit(lastVisit != null ? lastVisit.toString() : null); 
        // Calculate average visit amount
        if (totalVisits != null && totalVisits > 0 && totalDeduction != null) {
            dto.setAvgVisitAmount(totalDeduction / totalVisits);
        } else {
            dto.setAvgVisitAmount(0.0);
        }
        
        // Determine status
        dto.setStatus(determineUserStatus(lastVisit));
        
        // Get last recharge date
        String lastRechargeDate = getLastRechargeDate(userId);
        dto.setLastRechargeDate(lastRechargeDate);
        
        // Get recent transactions (last 10)
        List<TransactionEntity> recentTxs = txRepo.findTop10ByUserIdOrderByTimestampDesc(userId);
        List<TransactionDTO> txDTOs = recentTxs.stream()
            .map(this::convertToTransactionDTO)
            .collect(Collectors.toList());
        dto.setRecentTransactions(txDTOs);
        
        // Get recharge history
        List<RechargeHistoryDTO> rechargeHistory = getRechargeHistory(userId);
        dto.setRechargeHistory(rechargeHistory);
        
        return dto;
    }

    public UserStatsDTO getUserStats() {
        UserStatsDTO stats = new UserStatsDTO();
        
        // Total users
        List<PlayBoxUser> allUsers = userRepo.findAll();
        stats.setTotalUsers(allUsers.size());
        
        // Active users (visited in last 30 days)
        long activeUsers = allUsers.stream()
            .filter(user -> {
                Instant lastVisit = txRepo.getLastVisit(user.getId());
                return isActiveUser(lastVisit);
            })
            .count();
        stats.setActiveUsers((int) activeUsers);
        
        // Total recharge & deduction
        Double totalRecharge = 0.0;
        Double totalDeduction = 0.0;
        Double totalBalance = 0.0;
        
        for (PlayBoxUser user : allUsers) {
            Double recharge = txRepo.getTotalRecharge(user.getId());
            Double deduction = txRepo.getTotalDeduction(user.getId());
            
            if (recharge != null) totalRecharge += recharge;
            if (deduction != null) totalDeduction += deduction;
            
            totalBalance += user.getBalance();
        }
        
        stats.setTotalRecharge(totalRecharge);
        stats.setTotalDeduction(totalDeduction);
        stats.setAvgBalance(allUsers.isEmpty() ? 0.0 : totalBalance / allUsers.size());
        
        // New users today
        ZoneId IST = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(IST);
        Instant start = today.atStartOfDay(IST).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(IST).toInstant();
        
        long newUsersToday = allUsers.stream()
            .filter(user -> {
                try {
                    Instant userCreated = Instant.parse(user.getCreatedAt());
                    return !userCreated.isBefore(start) && userCreated.isBefore(end);
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
        stats.setNewUsersToday((int) newUsersToday);
        
        return stats;
    }

    public List<UserSummaryDTO> searchUsers(String query) {
        List<PlayBoxUser> users = userRepo.searchUsers(query);
        
        return users.stream().map(user -> {
            UserSummaryDTO dto = new UserSummaryDTO();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setCurrentBalance(user.getBalance());
            dto.setRegistrationDate(
                LocalDateTime.parse(user.getCreatedAt())
            );
            
            // Basic stats
            Double totalRecharge = txRepo.getTotalRecharge(user.getId());
            Double totalDeduction = txRepo.getTotalDeduction(user.getId());
            Long totalVisits = txRepo.getTotalVisits(user.getId());
            Instant lastVisit = txRepo.getLastVisit(user.getId()); 
            
            dto.setTotalRecharge(totalRecharge != null ? totalRecharge : 0.0);
            dto.setTotalDeduction(totalDeduction != null ? totalDeduction : 0.0);
            dto.setTotalVisits(totalVisits != null ? totalVisits.intValue() : 0);
            dto.setLastVisit(lastVisit != null ? lastVisit.toString() : null); 
            dto.setStatus(determineUserStatus(lastVisit));
            
            return dto;
        }).collect(Collectors.toList());
    }

    /* ---------------- HELPER METHODS ---------------- */

    private String determineUserStatus(String lastVisit) {
        if (lastVisit == null || lastVisit.isEmpty()) {
            return "inactive";
        }
        
        try {
            Instant lastVisitInstant = Instant.parse(lastVisit);
            Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
            
            return lastVisitInstant.isAfter(thirtyDaysAgo) ? "active" : "inactive";
        } catch (Exception e) {
            return "inactive";
        }
    }

    private boolean isActiveUser(Instant lastVisit) {
        if (lastVisit == null) {
            return false;
        }
        
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        return lastVisit.isAfter(thirtyDaysAgo);
    }
    private String getLastRechargeDate(Integer userId) {
        List<TransactionEntity> rechargeTxs = txRepo.findByUserIdAndTypeOrderByTimestampDesc(userId, "ADD");
        if (!rechargeTxs.isEmpty()) {
            return rechargeTxs.get(0).getTimestamp().toString();
        }
        return null;
    }

    private List<RechargeHistoryDTO> getRechargeHistory(Integer userId) {
        List<Object[]> results = txRepo.getRechargeHistory(userId);
        
        return results.stream()
            .map(r -> new RechargeHistoryDTO(
                r[0].toString(),  // date
                ((Number) r[1]).doubleValue()  // amount
            ))
            .collect(Collectors.toList());
    }

    private TransactionDTO convertToTransactionDTO(TransactionEntity tx) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(tx.getId());
        dto.setUserId(tx.getUserId());
        dto.setUserName(tx.getUserName());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setDescription(tx.getDescription());
        dto.setTimestamp(tx.getTimestamp().toString());
        dto.setAdminName(tx.getAdminName());
        dto.setBalanceAfter(tx.getBalanceAfter());
        return dto;
    }
}
