package com.filemanager.controllers;

import com.filemanager.models.Setting;
import com.filemanager.repositories.LogRepository;
import com.filemanager.repositories.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/setting")
public class SettingController {
    private final LogRepository logRepository;
    private final SettingRepository settingRepository;
    private final Logger logger = LoggerFactory.getLogger(SettingController.class);

    public SettingController(LogRepository logRepository, SettingRepository settingRepository) {
        this.logRepository = logRepository;
        this.settingRepository = settingRepository;
    }

    @GetMapping(value="list")
    public String getAll(Model model){
        List<Setting> settings = settingRepository.findAll();
        model.addAttribute("settings", settings);
        return "settings";
    }

    @PostMapping(value = "save")
    public String saveSettings(RedirectAttributes attributes){
        return "redirect:/setting/list";
    }
}
