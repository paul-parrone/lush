package com.px3j.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import reactivefeign.spring.config.EnableReactiveFeignClients;

/**
 * This is a simple Spring Boot application showing how to use Lush in your application.
 *
 * @author Paul Parrone
 */
@Slf4j
@SpringBootApplication
@EnableReactiveFeignClients
@ComponentScan({
        "com.px3j.lush.core",
        "com.px3j.lush.webflux",
        "com.px3j.example.service"
})
public class LushExampleServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(LushExampleServiceApp.class, args);
    }
}
