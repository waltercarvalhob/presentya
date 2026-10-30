package br.com.presentya.web.admin;

import br.com.presentya.domain.StatusPedido;
import br.com.presentya.dto.AdminDtos.DashboardDTO;
import br.com.presentya.dto.PedidoDtos.AlterarStatusRequest;
import br.com.presentya.dto.PedidoDtos.PedidoDTO;
import br.com.presentya.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** ADMIN e TECNICO acompanham pedidos, produção e entrega. */
@RestController
@RequestMapping("/api/admin")
public class AdminPedidoController {
    private final PedidoService pedidos;

    public AdminPedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/painel")
    public DashboardDTO painel() {
        return pedidos.painel();
    }

    @GetMapping("/pedidos")
    public List<PedidoDTO> listar(@RequestParam(required = false) StatusPedido status) {
        return pedidos.listar(status);
    }

    @PatchMapping("/pedidos/{codigo}/status")
    public PedidoDTO alterarStatus(@PathVariable String codigo, @Valid @RequestBody AlterarStatusRequest req) {
        return pedidos.alterarStatus(codigo, req.status());
    }

    /** Restrito a ADMIN (ver SecurityConfig): apaga de vez, por exemplo pedidos de teste. */
    @DeleteMapping("/pedidos/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String codigo) {
        pedidos.excluir(codigo);
    }
}
