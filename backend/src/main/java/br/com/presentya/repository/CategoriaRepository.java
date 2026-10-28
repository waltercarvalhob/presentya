package br.com.presentya.repository;

import br.com.presentya.domain.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByOrdemAscNomeAsc();
    boolean existsBySlug(String slug);
    boolean existsByPaiId(Long paiId);
}
