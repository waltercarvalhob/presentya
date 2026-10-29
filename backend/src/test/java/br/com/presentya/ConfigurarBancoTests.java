package br.com.presentya;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigurarBancoTests {

	@AfterEach
	void limpar() {
		System.clearProperty("spring.datasource.url");
		System.clearProperty("spring.datasource.username");
		System.clearProperty("spring.datasource.password");
	}

	@Test
	void converteEnderecoDoNeon() {
		PresentyaApiApplication.configurarBanco(
				"psql 'postgresql://neondb_owner:Ab%40c1@ep-x.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require'");
		assertEquals("jdbc:postgresql://ep-x.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require",
				System.getProperty("spring.datasource.url"));
		assertEquals("neondb_owner", System.getProperty("spring.datasource.username"));
		assertEquals("Ab@c1", System.getProperty("spring.datasource.password"));
	}

	@Test
	void mantemEnderecoJdbc() {
		PresentyaApiApplication.configurarBanco(" jdbc:postgresql://localhost:5432/BDPresentya ");
		assertEquals("jdbc:postgresql://localhost:5432/BDPresentya", System.getProperty("spring.datasource.url"));
	}

}
