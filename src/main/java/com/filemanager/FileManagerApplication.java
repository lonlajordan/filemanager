package com.filemanager;

import com.filemanager.enums.Role;
import com.filemanager.models.User;
import com.filemanager.repositories.SettingRepository;
import com.filemanager.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@SpringBootApplication
public class FileManagerApplication extends SpringBootServletInitializer implements CommandLineRunner {
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;

    public FileManagerApplication(SettingRepository settingRepository, UserRepository userRepository) {
        this.settingRepository = settingRepository;
        this.userRepository = userRepository;
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
        User user = userRepository.findByUsername("admin");
        if(user == null){
            user = new User("admin", new BCryptPasswordEncoder().encode("admin@123"), Role.ROLE_ADMIN.name());
            userRepository.save(user);
        }
    }
}
