package com.meteoriqs.foodswing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PlanningApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PlanningApp.class);
        application.addListeners(new ApplicationPidFileWriter());
        application.setWebApplicationType(WebApplicationType.REACTIVE);
        application.run(args);
    }
}
