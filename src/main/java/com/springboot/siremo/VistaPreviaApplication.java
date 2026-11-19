package com.springboot.siremo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VistaPreviaApplication {

	public static void main(String[] args) {
		SpringApplication.run(VistaPreviaApplication.class, args);
	}

}
