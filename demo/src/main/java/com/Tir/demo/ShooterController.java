package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.ArrayList;
import java.util.List;

@Controller
public class ShooterController {

    private final Club club;
    private  List<Shooter> shooters = new ArrayList<>();
    private int nextId = 1;
    
    public ShooterController(Club club){
        this.club = club;
        shooters.add(new Shooter(nextId++, "xxxx", "yyy", club.getId()));
        shooters.add(new Shooter(nextId++, "xxxx", "yyy", club.getId()));
    }
    @GetMapping("/tireurs")
    public String ListShooters(Model model) {
        model.addAttribute("club", club);
        model.addAttribute("shooters", shooters);
        return "tireurs";
    }
    @GetMapping("/tireurs/ajouter")
    public String showAddForm(){
        return "ajouter";
    }

    @PostMapping("/tireurs/ajouter")
    public String addshooter (@RequestParam String firstName, @RequestParam String lastName) {
        shooters.add(new Shooter(nextId++, firstName, lastName, club.getId()));
        return "redirect:/tireurs";
    }
    @GetMapping("/tireurs/supprimer/{id}")
    public String deleteShooter(@PathVariable int id) {
        shooters.removeIf(tireur -> tireur.getId() == id);
        return "redirect:/tireurs";
    }
}
    

    

    

















