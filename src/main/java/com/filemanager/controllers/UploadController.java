package com.filemanager.controllers;

import com.filemanager.models.Log;
import com.filemanager.models.Notification;
import com.filemanager.models.Setting;
import com.filemanager.models.User;
import com.filemanager.repositories.LogRepository;
import com.filemanager.repositories.SettingRepository;
import com.filemanager.services.EmailHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/upload")
public class UploadController {

    @Value("${root.working.directory}")
    private String ROOT_WORKING_DIRECTORY;

    private final LogRepository logRepository;
    private final SettingRepository settingRepository;

    public UploadController(LogRepository logRepository, SettingRepository settingRepository) {
        this.logRepository = logRepository;
        this.settingRepository = settingRepository;
    }

    @PostMapping(value = "files")
    public String uploadFiles(@RequestParam MultipartFile[] files, @RequestParam(required = false, defaultValue = "") String institution, @RequestParam(required = false, defaultValue = "") String destination, RedirectAttributes attributes, Principal principal){
        List<Setting> settings = settingRepository.findAll();
        String SYS_ALERT_MAIL = settings.stream().filter(setting -> "sys.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String CBC_ALERT_MAIL = settings.stream().filter(setting -> "cbc.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String CBT_ALERT_MAIL = settings.stream().filter(setting -> "cbt.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String GIE_ALERT_MAIL = settings.stream().filter(setting -> "gie.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        int length, index;
        long timer, lastTimer = 0;
        String name, operator = "", day = "", month, year = "";
        Set<String> days = new HashSet<>();
        List<String> months = Arrays.asList("JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN", "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE");
        for(MultipartFile file: files){
            if(StringUtils.isEmpty(institution)) break;
            name = StringUtils.defaultString(file.getOriginalFilename()).toUpperCase();
            if(name.contains("NEW")) continue;
            index = name.lastIndexOf(".");
            if(index > 0 && name.substring(index).matches(".*[a-zA-Z].*")) name = name.substring(0, index);
            String[] blocks = name.split("\\.")[0].split("_");
            String formatDate = blocks[blocks.length - 1];
            length = formatDate.length();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            if(name.toLowerCase().contains("ep745") || name.toLowerCase().contains("compen")){
                try {
                    day = formatDate.substring(0, 2);
                    month = formatDate.substring(2, 4);
                    year = (Calendar.getInstance().get(Calendar.YEAR) + "").substring(0, 2) + formatDate.substring(length - 2);
                    Date date = formatter.parse(day + "-" + month + "-" + year);
                    timer = date.toInstant().getEpochSecond();
                    lastTimer = Math.max(timer, lastTimer);
                    days.add(day);
                } catch (Exception e) {
                    logRepository.save(Log.error("Erreur lors de la détermination de la date des fichiers téléversés", ExceptionUtils.getStackTrace(e)));
                }
                operator = "VISA";
            }else if(name.matches("(CBT|CBC)?[0-9]+")){
                try {
                    day = formatDate.substring(length - 2);
                    month = formatDate.substring(length - 4, length - 2);
                    year = (Calendar.getInstance().get(Calendar.YEAR) + "").substring(0, 2) + formatDate.substring(length - 6, length - 4);
                    Date date = formatter.parse(day + "-" + month + "-" + year);
                    timer = date.toInstant().getEpochSecond();
                    lastTimer = Math.max(timer, lastTimer);
                    days.add(day);
                } catch (Exception e) {
                    logRepository.save(Log.error("Erreur lors de la détermination de la date des fichiers téléversés", ExceptionUtils.getStackTrace(e)));
                }
                operator = "GIMAC";
            }else if(name.toLowerCase().contains("application")){
                operator = "APPLICATION";
                day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "";
                if(day.length() < 2) day = "0" + day;
                days.add(day);
                lastTimer = Instant.now().getEpochSecond();
                break;
            }
        }
        operator = StringUtils.defaultString(operator, "GIMAC");
        List<String> dayList = new ArrayList<>(days);
        Collections.sort(dayList);
        String root = ROOT_WORKING_DIRECTORY + File.separator + "CBC";
        if("CBC".equalsIgnoreCase(institution)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "CBC";
        }else if("CBT".equalsIgnoreCase(institution)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "CBT";
        }
        LocalDate date = Instant.ofEpochSecond(lastTimer).atZone(ZoneId.systemDefault()).toLocalDate();
        Path path = Paths.get(root, operator, date.getYear() + "", months.get(date.getMonthValue() - 1), String.join("-", dayList));
        if("APPLICATION".equalsIgnoreCase(operator)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "GIEGCB";
            path = Paths.get(root);
        }else if(StringUtils.isEmpty(institution)){
            try {
                path = Paths.get(URLDecoder.decode(destination, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                attributes.addAttribute("p", path.toAbsolutePath().toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        File folder = new File(path.toUri());
        if (!folder.exists() && !folder.mkdirs()) throw new SecurityException("Erreur lors de la création du dossier de sauvegarde des fichiers.");
        List<String> fileNames = new ArrayList<>();
        int count = 0;
        for(MultipartFile file: files){
            name = StringUtils.defaultString(file.getOriginalFilename());
            fileNames.add(name);
            if (name.length() > 0) {
                try {
                    File serverFile = new File(folder.getAbsolutePath() + File.separator + name);
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
                    stream.write(file.getBytes());
                    stream.close();
                    count++;
                } catch (Exception e) {
                    logRepository.save(Log.error("Erreur lors de l'enregistrement des fichiers téléversés sur le serveur", ExceptionUtils.getStackTrace(e)));
                }
            }
        }
        if(count == files.length){
            String plural = count == 1 ? "" : "s";
            attributes.addFlashAttribute("notification", new Notification("success", count + " fichier" + plural + " envoyé" + plural + "."));
        }else{
            count = files.length - count;
            String plural = count == 1 ? "" : "s";
            attributes.addFlashAttribute("notification", new Notification("error", count + " fichier" + plural + " non envoyé" + plural + "."));
        }
        String cc = "";
        String to = "";
        String subject = "";
        String body = "";
        if("GIMAC".equalsIgnoreCase(operator)){
            to = "CBC".equalsIgnoreCase(institution) ? CBC_ALERT_MAIL : CBT_ALERT_MAIL;
            cc = GIE_ALERT_MAIL;
            subject = "Rapports GIMAC disponibles";
            body = "-----------------------------------------------------------------------------------------<br><br>" +
                   "Bonjour,<br><br>" +
                   "Les rapports GIMAC du " + String.join("-", dayList) + " " + months.get(date.getMonthValue() - 1) + " " + year + " sont actuellement disponibles.<br><br>" +
                   "<b>Email envoyé automatiquement depuis le serveur SFTP GIMAC / VISA</b><br><br>" +
                   "<i>L'Equipe Support Monétique GIE GCB</i><br><br>" +
                   "-----------------------------------------------------------------------------------------";
        }else if("VISA".equalsIgnoreCase(operator)){
            to = "CBC".equalsIgnoreCase(institution) ? CBC_ALERT_MAIL : CBT_ALERT_MAIL;
            cc = GIE_ALERT_MAIL;
            subject = "Rapports VISA disponibles";
            body = "-----------------------------------------------------------------------------------------<br><br>" +
                   "Bonjour,<br><br>" +
                   "Les fichiers VIS du " + String.join("-", dayList) + " " + months.get(date.getMonthValue() - 1) + " " + year + " sont actuellement disponibles.<br><br>" +
                   "<b>Email envoyé automatiquement depuis le serveur SFTP GIMAC / VISA</b><br><br>" +
                   "<i>L'Equipe Support Monétique GIE GCB</i><br><br>" +
                   "-----------------------------------------------------------------------------------------";
        }else if("APPLICATION".equalsIgnoreCase(operator)){
            to = GIE_ALERT_MAIL;
            cc = "CBC".equalsIgnoreCase(institution) ? CBC_ALERT_MAIL : CBT_ALERT_MAIL;
            subject = "Intégration des fichiers applications";
            body = "-----------------------------------------------------------------------------------------<br><br>" +
                   "Bonjour,<br><br>";
            if(fileNames.size() == 1){
                body += "Nous vous prions d’intégrer le fichier application <b>" + fileNames.get(0) + "</b><br><br>";
            }else {
                body += "Nous vous prions d’intégrer les fichiers applications : <br><br>" +
                        "<ul>" +
                        fileNames.stream().map(n -> "<li><b>" + n + "</b></li>").collect(Collectors.joining("\n")) +
                        "</ul>";
            }
            body += "<b>Email envoyé automatiquement depuis le serveur SFTP GIMAC / VISA</b><br><br>" +
                    "<i>L'Equipe Support Monétique " + institution + "</i><br><br>" +
                    "-----------------------------------------------------------------------------------------";
        }
        if(StringUtils.isNotEmpty(operator)){
            try {
                EmailHelper.sendMail(SYS_ALERT_MAIL, to, cc, subject, body);
            } catch (MessagingException e) {
                logRepository.save(Log.error("Erreur lors de l'envoie du mail de notification", ExceptionUtils.getStackTrace(e)));
            }
            logRepository.save(Log.info("Des fichiers <b>" + operator + "</b> ont été déposés par <b>" + principal.getName() + "</b>"));
        }
        return "redirect:/home";
    }
}
