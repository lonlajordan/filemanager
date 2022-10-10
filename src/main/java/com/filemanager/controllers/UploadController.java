package com.filemanager.controllers;

import com.filemanager.enums.Institution;
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
    public String uploadFiles(@RequestParam MultipartFile[] files, @RequestParam(required = false, defaultValue = "") String institution, @RequestParam(required = false, defaultValue = "") String destination, RedirectAttributes attributes, Principal principal, HttpSession session){
        List<Setting> settings = settingRepository.findAll();
        String SYS_ALERT_MAIL = settings.stream().filter(setting -> "sys.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String CBC_ALERT_MAIL = settings.stream().filter(setting -> "cbc.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String CBT_ALERT_MAIL = settings.stream().filter(setting -> "cbt.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        String GIE_ALERT_MAIL = settings.stream().filter(setting -> "gie.alert.mail".equals(setting.getId())).findFirst().orElse(new Setting()).getValue();
        int length, index;
        boolean error = false, completion  = false;
        String name, operator = "", details = "";
        List<String> months = Arrays.asList("JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN", "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE");
        List<String> shortMonths = Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        List<Date> dates = new ArrayList<>();
        User user = (User) session.getAttribute("user");
        SimpleDateFormat formatter;
        for(MultipartFile file: files){
            if(StringUtils.isEmpty(institution)) break;
            name = StringUtils.defaultString(file.getOriginalFilename()).toUpperCase();
            if(name.contains("NEW")) continue;
            index = name.lastIndexOf(".");
            if(index > 0 && name.substring(index).matches(".*[a-zA-Z].*")) name = name.substring(0, index);
            String[] blocks = name.split("\\.")[0].split("[-_]");
            String formatDate = blocks[blocks.length - 1];
            for(int i = 1; i <= shortMonths.size(); i++){
                formatDate = formatDate.replaceAll(shortMonths.get(i-1), (i < 10 ? "0" : "") + i);
            }
            length = formatDate.length();
            if(name.toLowerCase().contains("ep745") || name.toLowerCase().contains("compen") || name.toLowerCase().contains("incom") || name.toLowerCase().contains("vss") || name.toLowerCase().contains("vis")){
                if(!operator.isEmpty() && !operator.equals("VISA")){
                    error = true;
                    details = "Impossible de téléverser simultanément les fichiers <b>" + operator + "</b> et <b>VISA</b>.";
                    break;
                }
                if(formatDate.length() == 8){
                    formatter = new SimpleDateFormat("ddMMyyyy");
                }else{
                    formatter = new SimpleDateFormat("ddMMyy");
                    formatDate = formatDate.substring(Math.max(0, length - 6));
                }
                try {
                    Date date = formatter.parse(formatDate);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(date);
                    if(!dates.isEmpty()){
                        Calendar calendar2 = Calendar.getInstance();
                        calendar2.setTime(dates.get(0));
                        int year1 = calendar1.get(Calendar.YEAR);
                        int year2 = calendar2.get(Calendar.YEAR);
                        int month1 = calendar1.get(Calendar.MONTH);
                        int month2 = calendar2.get(Calendar.MONTH);
                        if(year1 != year2){
                            error = true;
                            details = "Impossible de téléverser simultanément les fichiers VISA de <b>" + Math.min(year1, year2) + "</b> et <b>" + Math.max(year1, year2) + "</b>.";
                            break;
                        }else if(month1 != month2){
                            error = true;
                            details = "Impossible de téléverser simultanément les fichiers VISA de <b>" + months.get(Math.min(month1, month2)) + "</b> et <b>" + months.get(Math.max(month1, month2)) + "</b>.";
                            break;
                        }
                    }
                    dates.add(date);
                } catch (Exception e) {
                    logRepository.save(Log.error("Erreur lors de la détermination de la date des fichiers téléversés", ExceptionUtils.getStackTrace(e)));
                }
                operator = "VISA";
            }else if(name.matches("(CBT|CBC)?[0-9]+")){
                /*------ Rectify institution if the user makes a mistake while submitting upload form --------*/
                if(name.startsWith("CBC")) institution = "CBC";
                if(name.startsWith("CBT")) institution = "CBT";
                /*--------------------------------------------------------------------------------------------*/
                if(!operator.isEmpty() && !operator.equals("GIMAC")){
                    error = true;
                    details = "Impossible de téléverser simultanément les fichiers <b>" + operator + "</b> et <b>GIMAC</b>.";
                    break;
                }
                try {
                    formatter = new SimpleDateFormat("yyyyMMdd");
                    Date date = formatter.parse((Calendar.getInstance().get(Calendar.YEAR) + "").substring(0, 2) + formatDate.substring(length - 6));
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(date);
                    if(!dates.isEmpty()){
                        Calendar calendar2 = Calendar.getInstance();
                        calendar2.setTime(dates.get(0));
                        int year1 = calendar1.get(Calendar.YEAR);
                        int year2 = calendar2.get(Calendar.YEAR);
                        int month1 = calendar1.get(Calendar.MONTH);
                        int month2 = calendar2.get(Calendar.MONTH);
                        if(year1 != year2){
                            error = true;
                            details = "Impossible de téléverser simultanément les fichiers GIMAC de <b>" + Math.min(year1, year2) + "</b> et <b>" + Math.max(year1, year2) + "</b>.";
                            break;
                        }else if(month1 != month2){
                            error = true;
                            details = "Impossible de téléverser simultanément les fichiers GIMAC de <b>" + months.get(Math.min(month1, month2)) + "</b> et <b>" + months.get(Math.max(month1, month2)) + "</b>.";
                            break;
                        }
                    }
                    dates.add(date);
                } catch (Exception e) {
                    logRepository.save(Log.error("Erreur lors de la détermination de la date des fichiers téléversés", ExceptionUtils.getStackTrace(e)));
                }
                operator = "GIMAC";
            }else if(name.toLowerCase().contains("application")){
                operator = "APPLICATION";
                dates.add(new Date());
                break;
            }
        }
        if(StringUtils.isEmpty(operator) && StringUtils.isEmpty(destination)){
            error = true;
            details = "Seuls les fichiers <b>GIMAC</b>, <b>VISA</b> et <b>APPLICATION</b> sont autorisés.";
        }
        if(error){
            attributes.addFlashAttribute("notification", new Notification("error", details));
            return "redirect:/home";
        }
        Collections.sort(dates);
        Set<String> days = dates.stream().map(date -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return (calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + calendar.get(Calendar.DAY_OF_MONTH);
        }).collect(Collectors.toSet());
        String root = ROOT_WORKING_DIRECTORY + File.separator + "CBC";
        if("CBC".equalsIgnoreCase(institution)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "CBC";
        }else if("CBT".equalsIgnoreCase(institution)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "CBT";
        }
        Calendar calendar = Calendar.getInstance();
        if(!dates.isEmpty()) calendar.setTime(dates.get(0));
        Path path = Paths.get(root, operator, calendar.get(Calendar.YEAR) + "", months.get(calendar.get(Calendar.MONTH)), String.join("-", days));
        if(StringUtils.isEmpty(institution)){
            try {
                path = Paths.get(URLDecoder.decode(destination, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                attributes.addAttribute("p", path.toAbsolutePath().toString());
            } catch (UnsupportedEncodingException e) {
                logRepository.save(Log.error("Erreur lors de l'accès au répertoire des fichiers téléversés", ExceptionUtils.getStackTrace(e)));
            }
        }else if("APPLICATION".equalsIgnoreCase(operator)){
            root = ROOT_WORKING_DIRECTORY + File.separator + "GIEGCB";
            path = Paths.get(root, user.getInstitution().name());
        }
        File folder = new File(path.toUri());
        if(folder.exists()){
            completion = true;
        }else{
            File parent = folder.getParentFile();
            if(parent.exists()){
                for(File file: parent.listFiles()){
                    if(file.isDirectory() && file.getName().toLowerCase().contains(folder.getName())){
                        completion = true;
                        folder = file;
                        break;
                    }
                }
                if(!completion && !folder.mkdirs()) throw new SecurityException("Erreur lors de la création du dossier de sauvegarde des fichiers.");
            }else{
                if(!folder.mkdirs()) throw new SecurityException("Erreur lors de la création du dossier de sauvegarde des fichiers.");
            }
        }
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
                   (completion ? "Les fichiers manquants des rapports GIMAC du " : "Les rapports GIMAC du ") + String.join("-", days) + " " + months.get(calendar.get(Calendar.MONTH)) + " " + calendar.get(Calendar.YEAR) + " sont actuellement disponibles.<br><br>" +
                   "<b>Email envoyé automatiquement depuis FILE MANAGER</b><br><br>" +
                   "<i>L'Equipe Support Monétique GIE GCB</i><br><br>" +
                   "-----------------------------------------------------------------------------------------";
        }else if("VISA".equalsIgnoreCase(operator)){
            to = "CBC".equalsIgnoreCase(institution) ? CBC_ALERT_MAIL : CBT_ALERT_MAIL;
            cc = GIE_ALERT_MAIL;
            subject = "Rapports VISA disponibles";
            body = "-----------------------------------------------------------------------------------------<br><br>" +
                   "Bonjour,<br><br>" +
                   (completion ? "Les fichiers manquants des rapports VISA du " : "Les fichiers VIS du ") + String.join("-", days) + " " + months.get(calendar.get(Calendar.MONTH)) + " " + calendar.get(Calendar.YEAR) + " sont actuellement disponibles.<br><br>" +
                   "<b>Email envoyé automatiquement depuis FILE MANAGER</b><br><br>" +
                   "<i>L'Equipe Support Monétique GIE GCB</i><br><br>" +
                   "-----------------------------------------------------------------------------------------";
        }else if("APPLICATION".equalsIgnoreCase(operator)){
            to = GIE_ALERT_MAIL;
            cc = Institution.CBC.equals(user.getInstitution()) ? CBC_ALERT_MAIL : CBT_ALERT_MAIL;
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
            body += "<b>Email envoyé automatiquement depuis FILE MANAGER</b><br><br>" +
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
