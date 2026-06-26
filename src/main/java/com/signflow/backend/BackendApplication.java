package com.signflow.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {

//		SecureRandom random = new SecureRandom();
//		byte[] bytes = new byte[32];
//		random.nextBytes(bytes);
//		random.nextBytes(bytes);
//		String secret = Base64.getEncoder().encodeToString(bytes);
//		System.out.println("Secret Key: " + secret);

		SpringApplication.run(BackendApplication.class, args);


	}

}
