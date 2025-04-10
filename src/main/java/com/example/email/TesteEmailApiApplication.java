package com.example.email;

import com.example.email.service.EmailReaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class TesteEmailApiApplication implements CommandLineRunner {

    private final EmailReaderService emailReaderService;

    public TesteEmailApiApplication(EmailReaderService emailReaderService) {
        this.emailReaderService = emailReaderService;
    }

    public static void main(String[] args) {
        SpringApplication.run(TesteEmailApiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        emailReaderService.verificarEmails();
    }

}
