package com.filemanager.controllers;

import com.filemanager.models.FileItem;
import com.filemanager.models.Notification;
import com.filemanager.models.User;
import com.filemanager.security.SecurityConfig;
import com.filemanager.services.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.util.ArrayList;
import java.util.Collections;

@Controller
public class HomeController {
    @Value("${root.working.directory}")
    private String ROOT_WORKING_DIRECTORY;

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping(path = "/home")
    private String home(@RequestParam(required = false, defaultValue = "") String p, Model model, HttpSession session){
        File home = new File(getWorkingDirectory(session));
        File folder = new File(p.isEmpty() ? home.getAbsolutePath() : p);
        boolean isHome = home.getAbsolutePath().equalsIgnoreCase(folder.getAbsolutePath());
        ArrayList<FileItem> items = new ArrayList<>();
        int files = 0;
        int folders = 0;
        for(File file: folder.listFiles()){
            if(file.isHidden()) continue;
            try {
                items.add(new FileItem(file));
                if(file.isDirectory()){
                    folders++;
                }else{
                    files++;
                }
            } catch (IOException e) {
                logger.error("Error while reading [" + file.getAbsolutePath() + "] properties", e);
            }
        }
        try {
            model.addAttribute("currentDirectory", URLEncoder.encode(folder.getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
        } catch (UnsupportedEncodingException ignored) { }
        model.addAttribute("items", items);
        model.addAttribute("isHome", isHome);
        model.addAttribute("files", files);
        model.addAttribute("folders", folders);
        Path path = Paths.get(home.toURI());
        Path root = path.getRoot();
        try {
            FileStore store = Files.getFileStore(root);
            model.addAttribute("freeSpace", FileUtils.readableFileSize(store.getUsableSpace()));
            model.addAttribute("totalSpace", FileUtils.readableFileSize(store.getTotalSpace()));
        } catch (IOException ignored) {

        }
        if(!isHome) {
            try {
                model.addAttribute("parent", URLEncoder.encode(folder.getParentFile().getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
            } catch (UnsupportedEncodingException ignored) {

            }
            ArrayList<FileItem> directories = new ArrayList<>();
            File directory = folder;
            do {
                FileItem fileItem = new FileItem();
                fileItem.setFile(directory);
                try {
                    fileItem.setAbsolutePath(URLEncoder.encode(directory.getAbsolutePath(), String.valueOf(StandardCharsets.UTF_8)));
                } catch (UnsupportedEncodingException ignored) { }
                directories.add(fileItem);
                directory = directory.getParentFile();
            }while (!directory.getAbsolutePath().equalsIgnoreCase(home.getAbsolutePath()));
            Collections.reverse(directories);
            model.addAttribute("directories", directories);
        }
        return "index";
    }

    @GetMapping(path = "/rename")
    public String renameFile(@RequestParam String path, @RequestParam String name, RedirectAttributes attributes){
        try {
            Path sourcePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            String p = sourcePath.getParent().toAbsolutePath().toString();
            Path targetPath = sourcePath.getParent().resolve(name);
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            attributes.addAttribute("p", p);
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
        } catch (IOException e) {
            logger.error("renaming file error", e);
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/home";
    }

    @GetMapping(path = "/create")
    public String createFolder(@RequestParam String path, @RequestParam String name, RedirectAttributes attributes){
        try {
            Path parent = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            File folder = parent.resolve(name).toFile();
            if(!folder.exists()) folder.mkdirs();
            attributes.addAttribute("p", parent.toAbsolutePath().toString());
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
        } catch (IOException e) {
            logger.error("creating folder error", e);
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/home";
    }

    @RequestMapping(path = "/delete/files")
    public String deleteFiles(@RequestParam String[] paths, RedirectAttributes attributes){
        String p = "";
        for(String path: paths){
            try {
                Path filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                FileSystemUtils.deleteRecursively(filePath);
                if(StringUtils.isEmpty(p)) p = filePath.getParent().toAbsolutePath().toString();
            } catch (IOException e) {
                logger.error("file deletion error", e);
            }
        }
        attributes.addAttribute("p", p);
        return "redirect:/home";
    }

    public String getWorkingDirectory(HttpSession session){
        String directory = (String) session.getAttribute("directory");
        if(directory == null){
            directory = this.ROOT_WORKING_DIRECTORY;
            User user = (User) session.getAttribute("user");
            if(user != null){
                String username = StringUtils.defaultString(user.getUsername());
                if(username.equals(SecurityConfig.GIE_USERNAME)){
                    directory += File.separator + "giegcb";
                }else if(username.equals(SecurityConfig.CBC_USERNAME)){
                    directory += File.separator + "cbc";
                }else if(username.equals(SecurityConfig.CBT_USERNAME)){
                    directory += File.separator + "cbt";
                }
            }
            session.setAttribute("directory", directory);
        }
        return directory;
    }
}
