package com.springboot.siremo.core.config;

import com.springboot.siremo.core.responses.ErrorResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHandlingConfig {

    @Bean
    public ErrorResponseFactory errorResponseFactory() {
        return new ErrorResponseFactory();
    }
}
