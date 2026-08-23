package com.java.PayGuard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PayGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayGuardApplication.class, args);
	}

}
