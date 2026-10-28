package br.com.presentya.repository;

import br.com.presentya.domain.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByAtivoTrueOrderByVendidosDesc();
    List<Produto> findAllByOrderByIdDesc();
    long countByAtivoTrue();
    boolean existsByCategoriaId(Long categoriaId);
}
