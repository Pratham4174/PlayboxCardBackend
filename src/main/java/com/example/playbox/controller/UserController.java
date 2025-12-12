package com.example.playbox.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.playbox.model.PlayBoxUser;
import com.example.playbox.service.UserServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping("/card/{cardUid}")
    public PlayBoxUser getByCard(@PathVariable String cardUid) {
        return userService.getByCardUid(cardUid);
    }

    @PostMapping("/create")
    public PlayBoxUser create(@RequestBody PlayBoxUser user) {
        return userService.createUser(user);
    }

    @PostMapping("/add")
    public PlayBoxUser addBalance(
            @RequestParam String cardUid,
            @RequestParam float amount
    ) {
        return userService.addBalance(cardUid, amount);
    }

    @PostMapping("/deduct")
    public PlayBoxUser deductBalance(
            @RequestParam String cardUid,
            @RequestParam float amount
    ) {
        return userService.deductBalance(cardUid, amount);
    }

    @GetMapping("/all")
   public List<PlayBoxUser> getAllUsers() {
    return userService.getAllUsers();
}

@GetMapping("/phone/{phone}")
public PlayBoxUser getByPhone(@PathVariable String phone) {
    return userService.getByPhone(phone);
}

}

