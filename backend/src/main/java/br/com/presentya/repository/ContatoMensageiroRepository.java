package br.com.presentya.repository;

import br.com.presentya.domain.ContatoMensageiro;
import br.com.presentya.domain.ContatoMensageiro.Canal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ContatoMensageiroRepository extends JpaRepository<ContatoMensageiro, Long> {
    Optional<ContatoMensageiro> findByCanalAndDestino(Canal canal, String destino);
    List<ContatoMensageiro> findAllByCanal(Canal canal);
    long countByCanal(Canal canal);

    @Transactional
    void deleteByCanalAndDestino(Canal canal, String destino);
}
