package com.meteoriqs.foodswing.config;

import com.meteoriqs.foodswing.common.util.Oauth2ConverterUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder
                .withJwkSetUri("http://68.178.171.57/auth")
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, Oauth2ConverterUtil util) {
        http.authorizeExchange(exchange -> exchange
                .pathMatchers("/userInfo").hasAuthority("ROLE_PROFILE")
                .pathMatchers("/api/**").hasAuthority("ROLE_MDM_READ")
        ).oauth2ResourceServer(util.getOauth2ResourceServerSpecCustomizer()); //Customizer.withDefaults()
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}
