package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.models.Notification;
import com.filemanager.repositories.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/log")
public class LogController {
    private final LogRepository logRepository;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public LogController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping(value="list")
    public String getAll(@RequestParam(required = false, defaultValue = "1") int p, Model model){
        Pageable pageable = PageRequest.of(p  - 1, 50);
        Page<Log> logs = logRepository.findAllByOrderByDateDesc(pageable);
        model.addAttribute("logs", logs.get().collect(Collectors.toList()));
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("currentPage", p);
        return "logs";
    }

    @PostMapping(value="delete")
    public String deleteLogs(@RequestParam Integer[] ids, RedirectAttributes attributes){
        try {
            logRepository.deleteAllById(Arrays.asList(ids));
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
        }catch (Exception e){
            logger.error("error while deleting logs", e);
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/log/list";
    }
}
