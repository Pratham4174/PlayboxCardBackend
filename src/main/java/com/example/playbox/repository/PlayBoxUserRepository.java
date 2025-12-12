package com.example.playbox.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.playbox.model.PlayBoxUser;

public interface PlayBoxUserRepository extends JpaRepository<PlayBoxUser, Long> {

    Optional<PlayBoxUser> findByCardUid(String cardUid);
    PlayBoxUser findByPhone(String phone);

}