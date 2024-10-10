package ${package};


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * This is a simple Spring Boot application showing how to use Lush in your application.
 *
 * @author Paul Parrone
 */
@Slf4j
@SpringBootApplication
@ComponentScan( {
        "com.px3j.lush.core",
        "com.px3j.lush.web",
        "${package}"
})
public class LushServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(LushServiceApp.class, args);
    }
}
