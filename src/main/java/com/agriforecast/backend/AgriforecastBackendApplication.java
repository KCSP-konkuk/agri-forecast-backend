package com.agriforecast.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgriforecastBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgriforecastBackendApplication.class, args);
	}

}
