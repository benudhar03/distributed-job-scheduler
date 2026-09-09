package com.ar.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArSchedulingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArSchedulingServiceApplication.class, args);
	}

}
