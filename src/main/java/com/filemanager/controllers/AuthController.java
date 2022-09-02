package com.filemanager.controllers;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {
    @GetMapping("/")
    public String login() {
        return  isAuthenticated() ? "redirect:home" : "login" ;
    }

    @PostMapping("/")
    public ModelAndView login(@RequestParam(required = false, defaultValue = "") String error, @RequestParam String username, @RequestParam String password) {
        ModelAndView context = new ModelAndView();
        context.setViewName("login");
        context.getModel().put("username", username);
        context.getModel().put("password", password);
        if(error != null && !error.isEmpty()){
            context.getModel().put("error", true);
            String message = "Une erreur s'est produite. Réessayez plutard.";
            if("1".equalsIgnoreCase(error)){
                message = "Utilisateur introuvable";
            }else if("2".equalsIgnoreCase(error)){
                message = "Mot de passe incorrect";
            }
            context.getModel().put("message", message);
        }
        return context;
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) return false;
        return authentication.isAuthenticated();
    }
}
