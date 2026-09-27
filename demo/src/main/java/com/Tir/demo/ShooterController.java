package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.ArrayList;
import java.util.List;

@Controller
public class ShooterController {

    private  List<Shooter> shooters = new ArrayList<>();
    private int nextId = 1;
    
    public ShooterController(){
        shooters.add(new Shooter(nextId++, "xxxx", "yyy"));
        shooters.add(new Shooter(nextId++, "xxxx", "yyy"));
    }
    @GetMapping("/tireurs")
    public String ListShooters(Model model) {
        model.addAttribute("shooters", shooters);
        return "tireurs";
    }
    @GetMapping("/tireurs/ajouter")
    public String showAddForm(){
        return "ajouter";
    }

    @PostMapping("/tireurs/ajouter")
    public String addshooter (@RequestParam String firstName, @RequestParam String lastName) {
        shooters.add(new Shooter(nextId++, firstName, lastName));
        return "redirect:/tireurs";
    }
}


















