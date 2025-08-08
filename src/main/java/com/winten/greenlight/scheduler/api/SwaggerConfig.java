package com.winten.greenlight.scheduler.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Value("${server.url}")
    private String serverUrl;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Greenlight")
                        .description("Greenlight Back Office Scheduler REST API.")
                        .version("1.0").contact(new Contact().name("Daniel Choi").email("danielchoi1115@gmail.com"))
                        .license(new License().name("License of API")
                                .url("API license URL"))
                )
                .servers(List.of(
                        new Server().url(serverUrl).description("Greenlight Back Office Scheduler Dev API 서버"),
                        new Server().url("http://localhost:"+serverPort).description("Greenlight Back Office Scheduler Localhost API 서버")
                ));
    }
}