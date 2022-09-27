package com.filemanager.controllers;

import com.filemanager.enums.Institution;
import com.filemanager.enums.Role;
import com.filemanager.models.FileItem;
import com.filemanager.models.Log;
import com.filemanager.models.Notification;
import com.filemanager.models.User;
import com.filemanager.repositories.LogRepository;
import com.filemanager.services.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    @Value("${root.working.directory}")
    private String ROOT_WORKING_DIRECTORY;

    private final LogRepository logRepository;

    public HomeController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping(path = "/home")
    private String home(@RequestParam(required = false, defaultValue = "") String p, Model model, HttpSession session){
        File home = new File(getWorkingDirectory(session));
        File folder = new File(p.isEmpty() ? home.getAbsolutePath() : p);
        boolean isHome = home.getAbsolutePath().equalsIgnoreCase(folder.getAbsolutePath());
        String action = (String) session.getAttribute("action");
        List<String> paths = (List<String>) session.getAttribute("paths");
        List<Path> pathList = (paths != null && !paths.isEmpty()) ? paths.stream().map(path -> {
            try {
                return Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            } catch (UnsupportedEncodingException ignored) { }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();
        ArrayList<FileItem> items = new ArrayList<>();
        int files = 0;
        int folders = 0;
        for(File file: folder.listFiles()){
            if(file.isHidden()) continue;
            try {
                if(action == null){
                    items.add(new FileItem(file));
                    if(file.isDirectory()){
                        folders++;
                    }else{
                        files++;
                    }
                }else if (file.isDirectory() && !pathList.contains(file.toPath())){
                    items.add(new FileItem(file));
                }
            } catch (IOException e) {
                logRepository.save(Log.error("Erreur lors de la lecture des propriétés de <b>" + file.getAbsolutePath() + "</b> properties", ExceptionUtils.getStackTrace(e)));
            }
        }
        try {
            model.addAttribute("currentDirectory", URLEncoder.encode(folder.getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
        } catch (UnsupportedEncodingException ignored) { }
        model.addAttribute("items", items);
        model.addAttribute("isHome", isHome);
        model.addAttribute("files", files);
        model.addAttribute("folders", folders);
        try {
            FileStore store = Files.getFileStore(Paths.get(home.toURI()));
            model.addAttribute("freeSpace", FileUtils.readableFileSize(store.getUsableSpace()));
            model.addAttribute("totalSpace", FileUtils.readableFileSize(store.getTotalSpace()));
        } catch (IOException e) {
            logRepository.save(Log.error("Erreur lors de la lecture des propriétés de la partition", ExceptionUtils.getStackTrace(e)));
        }
        if(!isHome) {
            try {
                model.addAttribute("parent", URLEncoder.encode(folder.getParentFile().getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
            } catch (UnsupportedEncodingException e) {
                logRepository.save(Log.error("Erreur lors de l'accès au répertoire parent", ExceptionUtils.getStackTrace(e)));
            }
            ArrayList<FileItem> directories = new ArrayList<>();
            File directory = folder;
            do {
                FileItem fileItem = new FileItem();
                fileItem.setFile(directory);
                try {
                    fileItem.setAbsolutePath(URLEncoder.encode(directory.getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
                } catch (UnsupportedEncodingException e) {
                    logRepository.save(Log.error("Erreur lors de la lecture du chemin absolu", ExceptionUtils.getStackTrace(e)));
                }
                directories.add(fileItem);
                directory = directory.getParentFile();
            }while (!directory.getAbsolutePath().equalsIgnoreCase(home.getAbsolutePath()));
            Collections.reverse(directories);
            model.addAttribute("directories", directories);
        }
        return "home";
    }

    @PostMapping(path = "/rename")
    public String renameFile(@RequestParam String path, @RequestParam String name, RedirectAttributes attributes, Principal principal){
        try {
            Path sourcePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            String p = sourcePath.getParent().toAbsolutePath().toString();
            Path targetPath = sourcePath.getParent().resolve(name);
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            attributes.addAttribute("p", p);
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
            logRepository.save(Log.info("Renommage de <b>" + sourcePath + "</b> en <b>" + targetPath + "</b> par <b>" + principal.getName() + "</b>"));
        } catch (IOException e) {
            logRepository.save(Log.error("Erreur lors du renommage", ExceptionUtils.getStackTrace(e)));
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/home";
    }

    @PostMapping(path = "/create")
    public String createFolder(@RequestParam String path, @RequestParam @NonNull String name, RedirectAttributes attributes, Principal principal){
        Path parent = null;
        try {
            name = name.trim().replaceAll("\\s+", "_").toUpperCase();
            parent = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            File folder = parent.resolve(name).toFile();
            if(!folder.exists()) folder.mkdirs();
            attributes.addAttribute("p", parent.toAbsolutePath().toString());
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
            logRepository.save(Log.info("Création du dossier <b>" + folder.toPath() + "</b> par <b>" + principal.getName() + "</b>"));
        } catch (IOException e) {
            logRepository.save(Log.error("Erreur lors de la création du dossier <b>" + name + "</b> dans <b>" + parent + "</b>", ExceptionUtils.getStackTrace(e)));
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/home";
    }

    @RequestMapping(path = "/delete/files")
    public String deleteFiles(@RequestParam String[] paths, RedirectAttributes attributes, Principal principal){
        String p = "";
        Path filePath = null;
        for(String path: paths){
            try {
                filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                FileSystemUtils.deleteRecursively(filePath);
                if(StringUtils.isEmpty(p)) p = filePath.getParent().toAbsolutePath().toString();
                attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
                logRepository.save(Log.info("Suppression de <b>" + filePath + "</b> par <b>" + principal.getName() + "</b>"));
            } catch (IOException e) {
                attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
                logRepository.save(Log.error("Erreur lors de la suppression de <b>" + filePath + "</b>", ExceptionUtils.getStackTrace(e)));
            }
        }
        attributes.addAttribute("p", p);
        return "redirect:/home";
    }

    @RequestMapping(path = "/move/files")
    public String moveFiles(@RequestParam String[] paths, @RequestParam String action, HttpSession session, RedirectAttributes attributes){
        String p = "";
        session.setAttribute("action", action);
        session.setAttribute("paths", Arrays.asList(paths));
        Notification notification = new Notification("success", paths.length + " élément" + (paths.length > 1 ? "s" : "") + " en attente de " + ("copy".equalsIgnoreCase(action) ? "copie" : "déplacement"));
        for(String path: paths){
            try {
                p = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize().getParent().toAbsolutePath().toString();
                break;
            } catch (IOException e) {
                logRepository.save(Log.error("Erreur lors de l'accès au répertoire parent", ExceptionUtils.getStackTrace(e)));
            }
        }
        attributes.addAttribute("p", p);
        attributes.addFlashAttribute("notification", notification);
        return "redirect:/home";
    }

    @RequestMapping(path = "/paste/files")
    public String pasteFiles(@RequestParam String destination, @RequestParam String action, HttpSession session, RedirectAttributes attributes){
        Path p = null;
        try {
            p = Paths.get(URLDecoder.decode(destination, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            attributes.addAttribute("p", p.toAbsolutePath().toString());
        } catch (UnsupportedEncodingException ignored) { }
        if("paste".equalsIgnoreCase(action) && p != null){
            String method = (String) session.getAttribute("action");
            List<String> paths = (List<String>) session.getAttribute("paths");
            if(method != null && paths != null && !paths.isEmpty()){
                List<Path> pathList = paths.stream().map(path -> {
                    try {
                        return Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                    } catch (UnsupportedEncodingException ignored) { }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
                final Path finalDestination = p;
                pathList.forEach(path -> {
                    File file = path.toFile();
                    if(file.exists() && !file.isHidden()){
                        try {
                            if(method.equalsIgnoreCase("copy")){
                                if(file.isDirectory()){
                                    org.apache.commons.io.FileUtils.copyDirectoryToDirectory(file, finalDestination.toFile());
                                }else{
                                    org.apache.commons.io.FileUtils.copyFileToDirectory(file, finalDestination.toFile());
                                }
                            }else{
                                if(file.isDirectory()){
                                    org.apache.commons.io.FileUtils.moveDirectoryToDirectory(file, finalDestination.toFile(), false);
                                }else{
                                    org.apache.commons.io.FileUtils.moveFileToDirectory(file, finalDestination.toFile(), false);
                                }
                            }
                        } catch (IOException e) {
                            logRepository.save(Log.error("Erreur lors d'une copie / déplacement", ExceptionUtils.getStackTrace(e)));
                            if(!attributes.getFlashAttributes().containsKey("notification")) attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération"));
                        }
                    }
                });
            }
        }
        if(!attributes.getFlashAttributes().containsKey("notification")) attributes.addFlashAttribute("notification", new Notification("success", "cancel".equalsIgnoreCase(action) ? "Opération annulée" : "Opération terminée avec succès"));
        session.removeAttribute("action");
        session.removeAttribute("paths");
        return "redirect:/home";
    }

    public String getWorkingDirectory(HttpSession session){
        String directory = (String) session.getAttribute("directory");
        if(directory == null){
            directory = this.ROOT_WORKING_DIRECTORY;
            User user = (User) session.getAttribute("user");
            if(user != null){
                if(Role.ROLE_GIE.equals(user.getRole())){
                    directory += File.separator + "GIEGCB";
                }else if(Institution.CBC.equals(user.getInstitution())){
                    directory += File.separator + "CBC";
                }else if(Institution.CBT.equals(user.getInstitution())){
                    directory += File.separator + "CBT";
                }
            }
            session.setAttribute("directory", directory);
        }
        return directory;
    }
}
