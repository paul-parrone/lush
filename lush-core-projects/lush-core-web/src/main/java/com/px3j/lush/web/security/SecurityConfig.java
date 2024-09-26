package com.px3j.lush.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j(topic = "lush.core.debug")
public class SecurityConfig {
    private final LushAuthenticationManager authenticationManager;
    private final LushSecurityContextRepository contextRepository;

    @Autowired
    public SecurityConfig(LushAuthenticationManager authenticationManager, LushSecurityContextRepository contextRepository) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @Value("${lush.security.protected-paths}")
    private List<String> protectedPaths;

    @Value("${lush.security.public-paths}")
    private List<String> publicPaths;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info( "****" );
        log.info("Lush Security Configuration: ");
        log.info( String.format("  - authenticationManager class: %s", authenticationManager.getClass().getName()) );
        log.info( String.format("  - contextRepository class: %s", contextRepository.getClass().getName()) );
        log.info( String.format("  - protected-paths: %s", String.join(",", protectedPaths)));
        log.info( String.format("  - public-paths: %s", String.join(",", publicPaths)));
        log.info("****");

        return http
                .csrf(Customizer.withDefaults())

                .authenticationManager( this.authenticationManager )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(contextRepository)
                )

                .authorizeHttpRequests( exchanges -> {
                    exchanges.requestMatchers(HttpMethod.OPTIONS).permitAll();
                    // Require role: lush-monitor for actuator endpoints.
                    exchanges.requestMatchers("/actuator/**", "/health/**" ).hasAuthority("lush-monitor");

                    publicPaths.forEach( p -> exchanges.requestMatchers(p).permitAll() );

                    protectedPaths.forEach( p -> exchanges.requestMatchers(p).authenticated() );
                })
                .build();
    }
}
