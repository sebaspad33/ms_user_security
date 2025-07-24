package pe.edu.vallegrande.user_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory(".")  // 👈 Ruta donde está tu `.env` (ajusta si es distinto)
				.ignoreIfMalformed()
				.ignoreIfMissing()    // opcional: evita error si no lo encuentra
				.load();

		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(UserServiceApplication.class, args);
	}
}
