package com.filemanager;

import com.filemanager.models.Setting;
import com.filemanager.repositories.SettingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SpringBootApplication
public class FileManagerApplication extends SpringBootServletInitializer implements CommandLineRunner {
    private final SettingRepository settingRepository;

    public FileManagerApplication(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(FileManagerApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(FileManagerApplication.class);
    }

    @Override
    public void run(String... args) {
        Map<String, String> map = Stream.of(new String[][] {
                { "sys.alert.mail", "Mail de notification Système" },
                { "cbc.alert.mail", "Mail de notification CBC" },
                { "cbt.alert.mail", "Mail de notification CBT" },
                { "gie.alert.mail", "Mail de notification GIE" },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
        for(Map.Entry<String, String> entry: map.entrySet()){
            Setting setting = settingRepository.findById(entry.getKey()).orElse(null);
            if(setting == null) settingRepository.save(new Setting(entry.getKey(), entry.getValue()));
        }
    }
}
