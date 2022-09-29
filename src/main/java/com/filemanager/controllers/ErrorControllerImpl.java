package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.models.User;
import com.filemanager.repositories.LogRepository;
import com.filemanager.repositories.UserRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;

@Controller
@ControllerAdvice
@Component
public class ErrorControllerImpl implements ErrorController {
    private final UserRepository userRepository;
    private final LogRepository logRepository;

    public ErrorControllerImpl(UserRepository userRepository, LogRepository logRepository) {
        this.userRepository = userRepository;
        this.logRepository = logRepository;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model, Principal principal, Exception exception) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String message = "Une erreur s'est produite lors de cette opération. Veuillez contacter votre administrateur.";
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            return "redirect:/error/" + statusCode;
        }
        getConnectedUser(request.getSession(), principal);
        model.addAttribute("title", "Erreur");
        model.addAttribute("details", message);
        logRepository.save(Log.error(message, ExceptionUtils.getStackTrace(exception)));
        return "error";
    }

    @RequestMapping("/error/{status}")
    public String handleError(@PathVariable Integer status, HttpSession session, Model model, Principal principal, Exception exception) {
        getConnectedUser(session, principal);
        String title = "Erreur";
        String details = "Une erreur s'est produite lors de cette opération. Veuillez contacter votre administrateur.";
        switch (status) {
            case 401:
            case 403:
                title = "Accès refusé";
                details = "Vous n'avez pas les droits pour accéder à cette page. Veuillez contacter votre administrateur.";
                break;
            case 404:
                title = "Page Introuvable";
                details = "La page ou la ressource sollicitée est introuvable.";
                break;
            case 500:
                title = "Erreur Serveur";
                details = "Une erreur s'est porduite sur le serveur.";
                break;
            default:
                break;
        }
        model.addAttribute("title", title);
        model.addAttribute("details", details);
        logRepository.save(Log.error(details, ExceptionUtils.getStackTrace(exception)));
        return "error";
    }

    @ExceptionHandler({NoHandlerFoundException.class, MethodArgumentTypeMismatchException.class})
    private String notFoundPage(HttpSession session, Principal principal){
        getConnectedUser(session, principal);
        return "redirect:/error/404";
    }

    public void getConnectedUser(HttpSession session, Principal principal){
        User user = (User) session.getAttribute("user");
        if(user == null && userRepository != null && principal != null){
            user = userRepository.findByUsername(principal.getName());
            session.setAttribute("user", user);
        }
    }
}
