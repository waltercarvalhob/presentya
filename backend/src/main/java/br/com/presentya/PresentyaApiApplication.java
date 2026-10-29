package br.com.presentya;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PresentyaApiApplication {

	public static void main(String[] args) {
		configurarBanco(System.getenv("DB_URL"));
		SpringApplication.run(PresentyaApiApplication.class, args);
	}

	/**
	 * Aceita o DB_URL no formato JDBC ou como o Neon/Render copiam
	 * (postgresql://usuario:senha@host/banco?sslmode=require, às vezes dentro de "psql '...'").
	 */
	static void configurarBanco(String dbUrl) {
		if (dbUrl == null || dbUrl.isBlank()) return;
		String url = dbUrl.trim();
		if (url.startsWith("psql ")) url = url.substring(5).trim();
		url = url.replaceAll("^['\"]+|['\"]+$", "");
		if (url.startsWith("jdbc:")) {
			System.setProperty("spring.datasource.url", url);
			return;
		}
		if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) return;

		URI uri = URI.create(url);
		String porta = uri.getPort() > 0 ? ":" + uri.getPort() : "";
		String consulta = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
		System.setProperty("spring.datasource.url",
				"jdbc:postgresql://" + uri.getHost() + porta + uri.getRawPath() + consulta);

		String credenciais = uri.getRawUserInfo();
		if (credenciais != null) {
			String[] partes = credenciais.split(":", 2);
			System.setProperty("spring.datasource.username", URLDecoder.decode(partes[0], StandardCharsets.UTF_8));
			if (partes.length > 1) {
				System.setProperty("spring.datasource.password", URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
			}
		}
	}

}
