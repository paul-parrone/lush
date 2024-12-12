package com.px3j.lush.core.config;

import com.px3j.lush.web.common.Constants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API with LushTicket")
                        .version("1.0")
                        .description("This API requires a custom authentication mechanism using a LushTicket."))
                .addSecurityItem(new SecurityRequirement().addList("LushTicketAuth")) // Reference the scheme below
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("LushTicketAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY) // Use API key type
                                        .in(SecurityScheme.In.HEADER) // LushTicket is passed as a header
                                        .name(Constants.TICKET_HEADER_NAME) // The header name
                                        .description("A custom header containing the LushTicket for authentication.")));
    }
}