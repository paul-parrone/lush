package ${package};

import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OperationCustomizer;
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
@ComponentScan( {
        "com.px3j.lush.core",
        "com.px3j.lush.webflux",
        "${package}"
})
public class LushServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(LushServiceApp.class, args);
    }

    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> operation.addParametersItem(
                new Parameter()
                        .in("header")
                        .required(true)
                        .description("Lush Ticket as JSON")
                        .name("x-lush-ticket")
        );
    }

}
