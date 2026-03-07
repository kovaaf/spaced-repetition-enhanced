package org.company.spacedrepetitionbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SpacedRepetitionBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpacedRepetitionBotApplication.class, args);
    }
}