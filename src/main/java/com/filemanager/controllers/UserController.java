package com.filemanager.controllers;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;
import com.filemanager.models.Notification;
import com.filemanager.models.User;
import com.filemanager.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @GetMapping(value="list")
    public String getAll(Model model){
        List<User> users = userRepository.findAllByOrderByLastLoginDesc();
        model.addAttribute("users", users);
        return "users";
    }

    @PostMapping(value="delete")
    public String deleteUsers(@RequestParam Integer[] ids, RedirectAttributes attributes){
        try {
            userRepository.deleteAllById(Arrays.asList(ids));
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
        }catch (Exception e){
            logger.error("error while deleting users", e);
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/user/list";
    }

    @RequestMapping(value="toggle/{id}")
    public String toggleUser(@PathVariable int id, RedirectAttributes attributes){
        Notification notification = new Notification("error", "utilisateur introuvable.");
        try {
            User user = userRepository.findById(id).orElse(null);
            if(user != null){
                user.setEnabled(!user.isEnabled());
                userRepository.save(user);
                notification.setType("success");
                notification.setMessage("<b>" + user.getUsername() + "</b> a été " + (user.isEnabled() ? "activé" : "désactivé") + " avec succès.");
            }
        }catch (Exception e){
            notification.setType("error");
            notification.setMessage("Erreur lors du changement de statut du l'utilisateur d'identifiant <b>" + id + "</b>.");
            logger.error(notification.getMessage(), e);
        }
        attributes.addFlashAttribute("notification", notification);
        return "redirect:/user/list";
    }

    @PostMapping(value = "save")
    public String saveUser(User user, RedirectAttributes attributes, HttpSession session){
        User user$ = user;
        boolean creation = true;
        if(user.getId() != null){
            Optional<User> _user = userRepository.findById(user.getId());
            if(_user.isPresent()){
                user$ = _user.get();
                user$.setUsername(user.getUsername());
                user$.setInstitution(user.getInstitution());
                user$.setRole(user.getRole());
                creation = false;
            }
        }
        if(Institution.GIE.equals(user$.getInstitution())) user$.setRole(Role.ROLE_GIE);
        user$.normalize();
        Notification notification = new Notification();
        try {
            userRepository.save(user$);
            notification.setType("success");
            notification.setMessage("<b>" + user$.getUsername() +"</b> a été " + (creation ? "ajouté." : "modifié."));
            if(!creation){
                user = (User) session.getAttribute("user");
                if(user != null && user.getId().equals(user$.getId())){
                    user$ = userRepository.findByUsername(user$.getUsername());
                    if(user$ != null){
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        auth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        session.setAttribute("user", user$);
                    }
                }
            }
            creation = true;
            user$ = new User();
        } catch (Exception e){
            notification.setType("error");
            notification.setMessage("Erreur lors de la " + (creation ? "création" : "modification") + " de l'utilisateur <b>[ " + user$.getUsername() + " ]</b>.");
            logger.error(notification.getMessage(), e);
        }

        attributes.addFlashAttribute("notification", notification);
        return "redirect:/user/list";
    }
}
