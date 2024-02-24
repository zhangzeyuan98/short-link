package com.zzy.shortlink.admin.controller;


import com.zzy.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserControllerTest {
    @Autowired
    UserService userService;

    @Test
    public void test() {
        System.out.println(userService.hasUsername("zzy"));
        System.out.println(userService.hasUsername("cyt"));
        System.out.println(userService.hasUsername("zsh"));
        System.out.println(userService.hasUsername("wcs"));
    }
}
