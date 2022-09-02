package com.filemanager.security;

import com.filemanager.models.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    public static String ADMIN_USERNAME;
    public static String ADMIN_PASSWORD;
    public static String GIE_USERNAME;
    public static String GIE_PASSWORD;
    public static String CBC_USERNAME;
    public static String CBC_PASSWORD;
    public static String CBT_USERNAME;
    public static String CBT_PASSWORD;

    @Value("${admin.username}")
    public void setAdminUsername(String adminUsername) {
        ADMIN_USERNAME = adminUsername;
    }

    @Value("${admin.password}")
    public void setAdminPassword(String adminPassword) {
        ADMIN_PASSWORD = adminPassword;
    }

    @Value("${gie.username}")
    public void setGieUsername(String gieUsername) {
        GIE_USERNAME = gieUsername;
    }

    @Value("${gie.password}")
    public void setGiePassword(String giePassword) {
        GIE_PASSWORD = giePassword;
    }

    @Value("${cbc.username}")
    public void setCbcUsername(String cbcUsername) {
        CBC_USERNAME = cbcUsername;
    }

    @Value("${cbc.password}")
    public void setCbcPassword(String cbcPassword) {
        CBC_PASSWORD = cbcPassword;
    }

    @Value("${cbt.username}")
    public void setCbtUsername(String cbtUsername) {
        CBT_USERNAME = cbtUsername;
    }

    @Value("${cbt.password}")
    public void setCbtPassword(String cbtPassword) {
        CBT_PASSWORD = cbtPassword;
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.csrf().disable()
            .sessionManagement()
                .maximumSessions(1).expiredUrl("/").and()
                .invalidSessionUrl("/")
                .and()
            .exceptionHandling()
                .accessDeniedPage("/error")
                .and()
            .formLogin()
                .loginPage("/")
                .loginProcessingUrl("/")
                .defaultSuccessUrl("/home", true)
                .failureHandler(new AuthenticationFailureHandler())
                .and()
            .logout()
                .deleteCookies("JSESSIONID")
                .logoutUrl("/logout")
                .logoutSuccessHandler(new AuthenticationLogoutSuccessHandler())
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .and()
            .authorizeRequests()
                .antMatchers("/", "/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .anyRequest().permitAll();
        return http.build();
    }

    @Component
    public static class CustomAuthenticationProvider implements AuthenticationProvider {

        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String username = StringUtils.defaultString(authentication.getName());
            String password = (String) authentication.getCredentials();
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if(username.equals(ADMIN_USERNAME)){
                if(!new BCryptPasswordEncoder().matches(password, ADMIN_PASSWORD)) throw new BadCredentialsException("incorrect.password");
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }else if(username.equals(GIE_USERNAME)){
                if(!new BCryptPasswordEncoder().matches(password, GIE_PASSWORD)) throw new BadCredentialsException("incorrect.password");
                authorities.add(new SimpleGrantedAuthority("ROLE_GIE"));
            }else if(username.equals(CBC_USERNAME)){
                if(!new BCryptPasswordEncoder().matches(password, CBC_PASSWORD)) throw new BadCredentialsException("incorrect.password");
                authorities.add(new SimpleGrantedAuthority("ROLE_CBC"));
            }else if(username.equals(CBT_USERNAME)){
                if(!new BCryptPasswordEncoder().matches(password, CBT_PASSWORD)) throw new BadCredentialsException("incorrect.password");
                authorities.add(new SimpleGrantedAuthority("ROLE_CBT"));
            }else{
                throw new BadCredentialsException("incorrect.username");
            }
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attributes.getRequest().getSession(true);
            session.setAttribute("user", new User(username, password));
            return new UsernamePasswordAuthenticationToken(username, password, authorities);
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return authentication.equals(UsernamePasswordAuthenticationToken.class);
        }

    }

    private static class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
            String url = "/";
            if(exception != null) {
                String message = exception.getMessage();
                if("incorrect.username".equals(message)){
                    url = "/?error=1";
                }else if("incorrect.password".equals(message)){
                    url = "/?error=2";
                }
            }
            RequestDispatcher dispatcher = request.getRequestDispatcher(url);
            dispatcher.forward(request, response);
        }
    }

    private static class AuthenticationLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }

}
