package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    private final Club club;

    public HelloController(Club club) {
        this.club = club;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("club", club);
        return "accueil";
    }
}
