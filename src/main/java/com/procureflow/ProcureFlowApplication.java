package com.procureflow;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class ProcureFlowApplication {

    public static void main(String[] args) {
        // Load local .env file if present without overriding existing environment variables
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("backend/.env");
        }
        if (envFile.exists()) {
            Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent() != null ? envFile.getParent() : ".")
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
        }

        SpringApplication.run(ProcureFlowApplication.class, args);
    }
}
