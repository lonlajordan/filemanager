package com.filemanager.controllers;

import com.filemanager.models.FileItem;
import com.filemanager.models.User;
import com.filemanager.security.SecurityConfig;
import com.filemanager.services.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    @Value("${admin.working.directory}")
    private String ADMIN_WORKING_DIRECTORY;
    @Value("${gie.working.directory}")
    private String GIE_WORKING_DIRECTORY;
    @Value("${cbc.working.directory}")
    private String CBC_WORKING_DIRECTORY;
    @Value("${cbt.working.directory}")
    private String CBT_WORKING_DIRECTORY;

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
                //logger.error("Error while reading [" + file.getAbsolutePath() + "] properties", e);
            }
        }
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
    public String renameFile(@RequestParam String path, @RequestParam String name){
        try {
            Path sourcePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
            Path targetPath = Paths.get(sourcePath.getParent().toString(), name);
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:home";
    }

    @GetMapping(path = "/delete")
    public String deleteFiles(@RequestParam String[] paths){
        for(String path: paths){
            try {
                Path filePath = Paths.get(URLDecoder.decode(path, String.valueOf(StandardCharsets.UTF_8))).toAbsolutePath().normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:home";
    }

    public String getWorkingDirectory(HttpSession session){
        String directory = (String) session.getAttribute("directory");
        if(directory == null){
            directory = this.GIE_WORKING_DIRECTORY;
            User user = (User) session.getAttribute("user");
            if(user != null){
                String username = StringUtils.defaultString(user.getUsername());
                if(username.equals(SecurityConfig.ADMIN_USERNAME)){
                    directory = this.ADMIN_WORKING_DIRECTORY;
                }else if(username.equals(SecurityConfig.GIE_USERNAME)){
                    directory = this.GIE_WORKING_DIRECTORY;
                }else if(username.equals(SecurityConfig.CBC_USERNAME)){
                    directory = this.CBC_WORKING_DIRECTORY;
                }else if(username.equals(SecurityConfig.CBT_USERNAME)){
                    directory = this.CBT_WORKING_DIRECTORY;
                }
            }
            session.setAttribute("directory", directory);
        }
        return directory;
    }
}
