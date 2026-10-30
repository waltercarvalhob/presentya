package br.com.presentya.repository;

import br.com.presentya.domain.Pedido;
import br.com.presentya.domain.StatusPedido;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Pedido> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    List<Pedido> findByUsuarioIsNullAndEmailConvidadoIgnoreCase(String emailConvidado);
    List<Pedido> findTop200ByOrderByCriadoEmDesc();
    List<Pedido> findTop200ByStatusOrderByCriadoEmDesc(StatusPedido status);
    long countByStatus(StatusPedido status);

    @Query("select coalesce(sum(p.total), 0) from Pedido p where p.status in :status")
    BigDecimal somarTotal(Collection<StatusPedido> status);
}
