package com.filemanager.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
public class EmailHelper {
    private static String MAIL_HOST;
    private static int MAIL_PORT;

    @Value("${spring.mail.host}")
    private void setMailHost(String host){
        MAIL_HOST = host;
    }

    @Value("${spring.mail.port}")
    private void setMailPort(int port){
        MAIL_PORT = port;
    }

    public static void sendSimpleMessage(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(purge(to));
        message.setSubject(subject);
        message.setText(text);
        final JavaMailSender emailSender = getJavaMailSender();
        new Thread(() -> emailSender.send(message)).start();
    }

    public static void sendMailWithAttachment(String from, String to, String cc, String subject, String body, String attachment) throws MessagingException {
        cc = purge(cc);
        final JavaMailSender emailSender = getJavaMailSender();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true,"UTF-8");
        helper.setFrom(from);
        helper.setTo(InternetAddress.parse(purge(to)));
        if(StringUtils.isNotEmpty(cc)) helper.setCc(InternetAddress.parse(cc));
        helper.setSubject(subject);
        helper.setText(body, true);
        FileSystemResource file = new FileSystemResource(new File(attachment));
        helper.addAttachment(StringUtils.defaultString(file.getFilename() , "Fichier de personnalisation"), file);
        new Thread(() -> emailSender.send(message)).start();

    }

    public static void sendMail(String from, String to, String cc, String subject, String body) throws MessagingException {
        cc = purge(cc);
        final JavaMailSender emailSender = getJavaMailSender();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false,"UTF-8");
        helper.setFrom(from);
        helper.setTo(InternetAddress.parse(purge(to)));
        if(StringUtils.isNotEmpty(cc)) helper.setCc(InternetAddress.parse(cc));
        helper.setSubject(subject);
        helper.setText(body, true);
        new Thread(() -> emailSender.send(message)).start();
    }


    public static JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(MAIL_HOST);
        mailSender.setPort(MAIL_PORT);
        mailSender.setProtocol("smtp");
        Properties props = new Properties();
        props.put("mail.smtp.auth", "false");
        props.put("mail.debug", "false");
        mailSender.setJavaMailProperties(props);
        return mailSender;
    }

    private static String purge(String emails){
        return String.join(",", Arrays.stream(emails.replaceAll(";", ",").split(",")).filter(mail -> !mail.isEmpty()).collect(Collectors.toSet()));
    }
}
