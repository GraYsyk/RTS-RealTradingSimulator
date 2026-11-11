package net.graysenko.RTS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class RtsApplication {
	public static void main(String[] args) {
		SpringApplication.run(RtsApplication.class, args);
	}
}
