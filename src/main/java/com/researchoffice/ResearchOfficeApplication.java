package com.researchoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResearchOfficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResearchOfficeApplication.class, args);
    }
}
