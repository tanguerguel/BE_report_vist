package com.springboot.siremo.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger, con el mismo formato que el back de SIREMO.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:7291}")
    private String serverPort;

    @Value("${openapi-server-url:localhost}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Siremo - Vista previa de fichas")
                        .description("Genera la hoja de una ficha de monitoreo en JSON, HTML y PDF")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url("http://" + serverUrl + ":" + serverPort)
                                .description("Servidor de Desarrollo"),
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor por defecto")));
    }
}
