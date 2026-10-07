package com.cleantrack.laundry_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LaundrySystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LaundrySystemApplication.class, args);
	}

}
