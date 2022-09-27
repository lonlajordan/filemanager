package com.filemanager.security;

import com.filemanager.repositories.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class Scheduler {
    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final LogRepository logRepository;

    @Value("${root.working.directory}")
    private String ROOT_WORKING_DIRECTORY;

    public Scheduler(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Scheduled(cron = "0 0 2 * * MON-FRI", zone = "GMT+1")
    public void archive(){
        File folder = new File(ROOT_WORKING_DIRECTORY + File.separator + "GIEGCB");
        File archive = folder.toPath().resolve("ARCHIVE").toFile();
        if(!archive.exists()) archive.mkdirs();
        for(File file: folder.listFiles()){
            if(!file.isHidden() && !file.isDirectory()){
                if(logRepository.countAllByMessageContaining(file.toPath().toString()) > 0){
                    try {
                        Files.move(file.toPath(), archive.toPath().resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                    }catch (Exception e){
                        logger.error("error while moving file", e);
                    }
                }
            }
        }
    }

}
