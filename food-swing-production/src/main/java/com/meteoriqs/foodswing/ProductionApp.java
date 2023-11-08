package com.meteoriqs.foodswing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;

@SpringBootApplication
public class ProductionApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ProductionApp.class);
        application.addListeners(new ApplicationPidFileWriter());
        application.setWebApplicationType(WebApplicationType.REACTIVE);
        application.run(args);
    }
}
