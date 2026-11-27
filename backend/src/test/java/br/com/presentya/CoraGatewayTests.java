package br.com.presentya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.presentya.pagamento.CoraGateway;
import org.junit.jupiter.api.Test;

class CoraGatewayTests {

    @Test
    void validaCpfECnpj() {
        assertTrue(CoraGateway.documentoValido("52998224725"));     // CPF válido de exemplo
        assertTrue(CoraGateway.documentoValido("11222333000181"));  // CNPJ válido de exemplo
        assertFalse(CoraGateway.documentoValido("52998224724"));
        assertFalse(CoraGateway.documentoValido("11111111111"));
        assertFalse(CoraGateway.documentoValido("529.982.247-25")); // o servidor limpa a máscara antes
        assertFalse(CoraGateway.documentoValido(null));
    }

    @Test
    void aceitaPemNumaLinhaSo() throws Exception {
        var m = CoraGateway.class.getDeclaredMethod("pem", String.class);
        m.setAccessible(true);
        assertEquals("-----BEGIN X-----\nabc\n-----END X-----",
                m.invoke(null, "  -----BEGIN X-----\\nabc\\n-----END X-----  "));
    }
}
