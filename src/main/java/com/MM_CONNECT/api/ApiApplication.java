package com.MM_CONNECT.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "BearerAuth",
        description = "JWT authentication",
        scheme = "bearer",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER
)
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
