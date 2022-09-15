package com.filemanager.controllers;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {
    @GetMapping("/")
    public String login() {
        return  isAuthenticated() ? "redirect:home" : "login" ;
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> login(@RequestParam(required = false, defaultValue = "") String error) {
        HashMap<String, Object> map = new HashMap<>();
        if(error != null && !error.isEmpty()){
            map.put("error", true);
            String message = "Une erreur s'est produite. Réessayez plutard.";
            if("1".equalsIgnoreCase(error)){
                message = "utilisateur introuvable";
            }else if("2".equalsIgnoreCase(error)){
                message = "mot de passe incorrect";
            }else if("3".equalsIgnoreCase(error)){
                message = "votre compte est désactivé";
            }else if("4".equalsIgnoreCase(error)){
                message = "serveur d'authentification indisponible";
            }
            map.put("message", message);
        }
        return map;
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) return false;
        return authentication.isAuthenticated();
    }
}
