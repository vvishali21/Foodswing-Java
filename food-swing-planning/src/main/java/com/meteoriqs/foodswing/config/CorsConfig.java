package com.meteoriqs.foodswing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class CorsConfig implements WebFluxConfigurer {
    @Bean
    @ConfigurationProperties(prefix = "spring.webflux.cors")
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }


    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        CorsProperties corsProps = corsProperties();
        corsRegistry.addMapping("/**")
                .allowedOrigins(corsProps.getAllowedOrigins())
                .allowedMethods(corsProps.getAllowedMethods())
                .allowedHeaders(corsProps.getAllowedHeaders())
                .maxAge(corsProps.getMaxAge());
    }
}
