package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.repositories.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/log")
public class LogController {
    private final LogRepository logRepository;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public LogController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping(value="list")
    public String getAll(Model model){
        List<Log> logs = logRepository.findAllByOrderByDateDesc();
        model.addAttribute("logs", logs);
        return "logs";
    }
}
