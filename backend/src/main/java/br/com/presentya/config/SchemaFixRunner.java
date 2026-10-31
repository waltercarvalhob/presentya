package br.com.presentya.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * O "ddl-auto=update" do Hibernate só cria tabela/coluna nova; não relaxa uma restrição NOT NULL
 * que já existia (caso de pedido.usuario_id, que passou a ser opcional para suportar a compra como
 * convidado). Ajustamos isso aqui uma vez na subida — idempotente, não falha se já estiver certo
 * ou se o banco não for PostgreSQL (ex.: H2 nos testes).
 */
@Component
public class SchemaFixRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaFixRunner.class);
    private final DataSource dataSource;

    public SchemaFixRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("ALTER TABLE pedido ALTER COLUMN usuario_id DROP NOT NULL");
            log.info("Coluna pedido.usuario_id ajustada para aceitar pedido de convidado.");
        } catch (Exception e) {
            log.debug("Ajuste de pedido.usuario_id não aplicado (já atualizado ou banco não é PostgreSQL): {}",
                    e.getMessage());
        }
    }
}
