package com.Tir.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ShooterController {

    @GetMapping("/tireurs")
    public String tireurs(Model model) {
        List<Shooter> shooters = new ArrayList<>();
        shooters.add(new Shooter(1, "Thomas", "Blumer"));
        shooters.add(new Shooter(2, "Chelo", "Beniters"));

        model.addAttribute("shooters", shooters);
        return "tireurs";
    }
}
