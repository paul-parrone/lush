package com.px3j.service;

import com.px3j.lush.web.EnableLushWeb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * This is a simple Spring Boot application showing how to use Lush in your application.
 *
 * @author Paul Parrone
 */
@Slf4j
//@AutoConfigureObservability
@SpringBootApplication
@EnableFeignClients
@EnableLushWeb
public class LushServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(LushServiceApp.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
//                .additionalInterceptors(loggingInterceptor)
                .build();
    }
}
