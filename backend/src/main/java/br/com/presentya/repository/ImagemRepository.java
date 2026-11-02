package br.com.presentya.repository;

import br.com.presentya.domain.Imagem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemRepository extends JpaRepository<Imagem, Long> {
    @Query("select i.contentType from Imagem i where i.id = :id")
    Optional<String> tipo(@Param("id") Long id);
}
