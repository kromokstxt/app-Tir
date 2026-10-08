package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "accueil";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
