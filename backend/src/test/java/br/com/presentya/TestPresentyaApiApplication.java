package br.com.presentya;

import org.springframework.boot.SpringApplication;

/**
 * Sobe a API com banco H2 em memória, sem precisar do PostgreSQL (dados somem ao parar).
 * Uso: mvnw spring-boot:test-run -Dspring-boot.run.profiles=test
 */
public class TestPresentyaApiApplication {
    public static void main(String[] args) {
        SpringApplication.from(PresentyaApiApplication::main).run(args);
    }
}
