package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.models.Notification;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

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
    public String saveSettings(RedirectAttributes attributes, HttpServletRequest request){
        Notification notification = new Notification("success", "Les paramètres ont été enregistrés avec succès.");
        for(Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()){
            try {
                Setting setting = settingRepository.findById(entry.getKey()).orElse(null);
                if(setting != null && entry.getValue() != null && entry.getValue().length > 0){
                    setting.setValue(entry.getValue()[0]);
                    settingRepository.save(setting);
                }
                logRepository.save(Log.info(notification.getMessage()));
            } catch (Exception exception){
                notification.setType("error");
                notification.setMessage("Erreur lors de l'enregistrement des paramètres.");
                logger.error("error while changing " + entry.getKey() + " parameter value", exception);
            }
        }
        attributes.addFlashAttribute("notification", notification);
        return "redirect:/setting/list";
    }
}
