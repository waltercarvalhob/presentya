package br.com.presentya.repository;

import br.com.presentya.domain.Oferta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findAllByOrderByOrdemAscCriadoEmDesc();
}
