package com.filemanager.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class Scheduler {
    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Scheduled(cron = "0 0 10 * * SUN", zone = "GMT+1")
    public void deleteUselessFiles(){

    }

    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "GMT+1")
    public void notifyForDelivery(){
    }

}
