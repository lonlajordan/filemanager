package com.filemanager.controllers;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;
import com.filemanager.models.Log;
import com.filemanager.models.Notification;
import com.filemanager.models.User;
import com.filemanager.repositories.LogRepository;
import com.filemanager.repositories.UserRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;
    private final LogRepository logRepository;

    public UserController(UserRepository userRepository, LogRepository logRepository) {
        this.userRepository = userRepository;
        this.logRepository = logRepository;
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
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
            logRepository.save(Log.error("Erreur lors de la suppression d'un groupe d'utilisateur", ExceptionUtils.getStackTrace(e)));
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
            logRepository.save(Log.error(notification.getMessage(), ExceptionUtils.getStackTrace(e)));
        }
        attributes.addFlashAttribute("notification", notification);
        return "redirect:/user/list";
    }

    @PostMapping(value = "save")
    public String saveUser(User user, @RequestParam(required = false, defaultValue = "ROLE_BANK_MONET") List<String> authorities, RedirectAttributes attributes, HttpSession session){
        User user$ = user;
        boolean creation = true;
        if(user.getId() != null){
            Optional<User> _user = userRepository.findById(user.getId());
            if(_user.isPresent()){
                user$ = _user.get();
                user$.setUsername(user.getUsername());
                user$.setInstitution(user.getInstitution());
                creation = false;
            }
        }
        if(Institution.GIE.equals(user$.getInstitution())){
            user$.setRoles(Role.ROLE_GIE.name());
        }else{
            user$.setRoles(String.join(";", authorities));
        }
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
                        Collection<SimpleGrantedAuthority> authorities$ = user$.getRoleList().stream().map(Enum::name).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                        auth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), authorities$);
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
            logRepository.save(Log.error(notification.getMessage(), ExceptionUtils.getStackTrace(e)));
        }

        attributes.addFlashAttribute("notification", notification);
        return "redirect:/user/list";
    }
}
