package services.tmitocar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TmitocarServiceApplication {
    public static void main(String[] args) {
		System.setProperty("server.servlet.context-path", "/tmitocar");
		SpringApplication.run(TmitocarServiceApplication.class, args);
	}
}
