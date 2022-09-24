package com.filemanager.security;

import com.filemanager.enums.Role;
import com.filemanager.models.User;
import com.filemanager.repositories.UserRepository;
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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    public static String ADMIN_USERNAME;
    public static String ADMIN_PASSWORD;
    private static String LDAP_PROVIDER_URL;
    private static String LDAP_PROVIDER_HOST;
    private static String LDAP_PROVIDER_PORT;
    private static String LDAP_PROVIDER_DOMAIN_COMPONENT;
    private static String LDAP_PROVIDER_DOMAIN_NAME;

    @Value("${ldap.provider.host}")
    public void setLdapProviderHost(String host){
        SecurityConfig.LDAP_PROVIDER_HOST= host;
    }

    @Value("${ldap.provider.port}")
    public void setLdapProviderPort(String port){
        SecurityConfig.LDAP_PROVIDER_PORT= port;
    }

    @Value("${ldap.provider.domain.name}")
    public void setLdapProviderDomainName(String name){
        SecurityConfig.LDAP_PROVIDER_DOMAIN_NAME = name;
    }

    @Value("${ldap.provider.domain.component}")
    public void setLdapProviderDomainComponent(String component){
        SecurityConfig.LDAP_PROVIDER_DOMAIN_COMPONENT = component;
    }

    @Value("${admin.username}")
    public void setAdminUsername(String adminUsername) {
        ADMIN_USERNAME = adminUsername;
    }

    @Value("${admin.password}")
    public void setAdminPassword(String adminPassword) {
        ADMIN_PASSWORD = adminPassword;
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
        private final UserRepository userRepository;

        public CustomAuthenticationProvider(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String username = authentication.getName();
            String password = (String) authentication.getCredentials();
            User user = new User();
            if(SecurityConfig.ADMIN_USERNAME.equals(username)){
                if(!new BCryptPasswordEncoder().matches(password, SecurityConfig.ADMIN_PASSWORD)) throw new BadCredentialsException("incorrect.password");
                user.setRole(Role.ROLE_ADMIN);
                user.setUsername(username);
                user.setId(0);
            }else{
                user = userRepository.findByUsername(username);

                if (user == null || !user.getUsername().equalsIgnoreCase(username)) {
                    throw new BadCredentialsException("incorrect.username");
                }

                /*Hashtable<String, String> environment = new Hashtable<>();
                environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                try {
                    List<InetAddress> addresses = Arrays.asList(InetAddress.getAllByName(SecurityConfig.LDAP_PROVIDER_HOST));
                    SecurityConfig.LDAP_PROVIDER_URL = addresses.stream().map(address -> "ldap://" + address.getHostAddress() + ":" + SecurityConfig.LDAP_PROVIDER_PORT + "/" + SecurityConfig.LDAP_PROVIDER_DOMAIN_COMPONENT).collect(Collectors.joining(" "));
                } catch (UnknownHostException e) {
                    SecurityConfig.LDAP_PROVIDER_URL = "ldap://" + SecurityConfig.LDAP_PROVIDER_DOMAIN_NAME + ":" + SecurityConfig.LDAP_PROVIDER_PORT + "/" + SecurityConfig.LDAP_PROVIDER_DOMAIN_COMPONENT;
                }
                environment.put(Context.PROVIDER_URL, SecurityConfig.LDAP_PROVIDER_URL);
                environment.put(Context.SECURITY_AUTHENTICATION, "simple");
                environment.put(Context.SECURITY_PRINCIPAL, username + "@" + SecurityConfig.LDAP_PROVIDER_DOMAIN_NAME);
                environment.put(Context.SECURITY_CREDENTIALS, password);
                try {
                    LdapContext context = new InitialLdapContext(environment, null);
                    context.close();
                } catch (NamingException e) {
                    String error = e.getExplanation() == null ? "" : e.toString().toLowerCase();
                    throw new BadCredentialsException(error.contains("connection refused") ? "connection.refused" : "incorrect.password");
                }*/

                if(!user.isEnabled()) throw new BadCredentialsException("account.disabled");
                user.setLastLogin(new Date());
                userRepository.save(user);
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attributes.getRequest().getSession(true);
            session.setAttribute("user", user);
            return new UsernamePasswordAuthenticationToken(username, password, Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
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
                }else if("account.disabled".equals(message)){
                    url = "/?error=3";
                }else if("connection.refused".equals(message)){
                    url = "/?error=4";
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
