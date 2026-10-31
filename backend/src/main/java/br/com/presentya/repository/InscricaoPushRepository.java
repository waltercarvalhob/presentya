package br.com.presentya.repository;

import br.com.presentya.domain.InscricaoPush;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface InscricaoPushRepository extends JpaRepository<InscricaoPush, Long> {
    Optional<InscricaoPush> findByEndpoint(String endpoint);

    @Transactional
    void deleteByEndpoint(String endpoint);
}
