package com.filemanager.security;

import com.filemanager.repositories.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public Scheduler(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "GMT+1")
    public void archive(){
        File folder = new File("");
        int count;
        for(File file: folder.listFiles()){
            if(!file.isHidden() && !file.isDirectory()){
                count = logRepository.countAllByMessageContaining(file.toPath().toString());
                try {
                    if(count > 0) Files.move(folder.toPath(), folder.toPath().getParent().resolve("ARCHIVE"), StandardCopyOption.REPLACE_EXISTING);
                }catch (Exception e){
                    logger.error("error while moving file", e);
                }
            }
        }
    }

}
