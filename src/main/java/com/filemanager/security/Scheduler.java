package com.filemanager.security;

import com.filemanager.enums.Institution;
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

    // Archive application files every day at midnight
    @Scheduled(cron = "@daily", zone = "GMT+1")
    public void archive(){
        File folder = new File(ROOT_WORKING_DIRECTORY + File.separator + "GIEGCB");
        File archive = folder.toPath().resolve("ARCHIVE").toFile();
        if(!archive.exists()) archive.mkdirs();
        for(Institution institution: Institution.values()){
            if(institution.equals(Institution.GIE)) continue;
            File source = folder.toPath().resolve(institution.name()).toFile();
            File target = archive.toPath().resolve(institution.name()).toFile();
            if(!target.exists()) target.mkdirs();
            if(source.exists()){
                for(File file: source.listFiles()){
                    if(!file.isHidden() && !file.isDirectory()){
                        if(logRepository.countAllByMessageContaining(file.toPath().toString()) > 0){
                            try {
                                Files.move(file.toPath(), target.toPath().resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                            }catch (Exception e){
                                logger.error("error while moving file", e);
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete all logs every first of each month at midnight
    @Scheduled(cron = "@monthly", zone = "GMT+1")
    public void deleteAllLogs(){
        archive();
        logRepository.deleteAll();
    }

}
