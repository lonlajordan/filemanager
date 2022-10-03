package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.repositories.LogRepository;
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

@Controller
@ControllerAdvice
@Component
public class ErrorControllerImpl implements ErrorController {
    private final LogRepository logRepository;

    public ErrorControllerImpl(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model, Exception exception) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String message = "Une erreur s'est produite lors de cette opération. Veuillez contacter votre administrateur.";
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            return "redirect:/error/" + statusCode;
        }
        model.addAttribute("title", "Erreur");
        model.addAttribute("details", message);
        logRepository.save(Log.error(message, ExceptionUtils.getStackTrace(exception)));
        return "error";
    }

    @RequestMapping("/error/{status}")
    public String handleError(@PathVariable Integer status, Model model, Exception exception) {
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
                logRepository.save(Log.error(details, ExceptionUtils.getStackTrace(exception)));
                break;
            default:
                break;
        }
        model.addAttribute("title", title);
        model.addAttribute("details", details);
        return "error";
    }

    @ExceptionHandler({NoHandlerFoundException.class, MethodArgumentTypeMismatchException.class})
    private String notFoundPage(){
        return "redirect:/error/404";
    }
}
