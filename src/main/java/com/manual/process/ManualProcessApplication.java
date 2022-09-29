package com.manual.process;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ManualProcessApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(ManualProcessApplication.class, args);
	}

}
