package br.com.presentya.repository;

import br.com.presentya.domain.AnuncioPendente;
import br.com.presentya.domain.AnuncioPendente.Tipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnuncioPendenteRepository extends JpaRepository<AnuncioPendente, Long> {
    boolean existsByTipoAndRefId(Tipo tipo, Long refId);
    List<AnuncioPendente> findAllByOrderByCriadoEmAsc();
}
