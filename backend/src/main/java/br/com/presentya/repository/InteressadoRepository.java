package br.com.presentya.repository;

import br.com.presentya.domain.Interessado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteressadoRepository extends JpaRepository<Interessado, Long> {
    List<Interessado> findAllByOrderByCriadoEmDesc();
    Optional<Interessado> findByWhatsapp(String whatsapp);
}
