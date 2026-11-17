
package ds.FinHut8.contrato;

import ds.FinHut8.contrato.dtos.ContratoCreateDTO;
import ds.FinHut8.contrato.dtos.ContratoResponseDTO;
import ds.FinHut8.contrato.dtos.ContratoStatusUpdateDTO;
import ds.FinHut8.contrato.dtos.ContratoUpdateDTO;
import ds.FinHut8.contrato.enums.StatusContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContratoControllerTest {

    private ContratoService service;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        service = mock(ContratoService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new ContratoController(service))
                .build();
    }

    private ContratoResponseDTO resposta() {
        return new ContratoResponseDTO(
                15, 3, "Empresa Alfa", null,
                new BigDecimal("6000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1),
                StatusContrato.RASCUNHO
        );
    }

    @Test
    void criaContratoRetorna201() throws Exception {
        when(service.criar(any(ContratoCreateDTO.class)))
                .thenReturn(resposta());

        mvc.perform(post("/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projetoId": 3,
                                    "clienteNome": "Empresa Alfa",
                                    "valorTotal": 6000.00,
                                    "dataInicio": "2026-08-01",
                                    "dataFim": "2026-12-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("RASCUNHO"));
    }

    @Test
    void listaContratosRetorna200() throws Exception {
        when(service.listar(null, null, null))
                .thenReturn(List.of(resposta()));

        mvc.perform(get("/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15));
    }

    @Test
    void consultaPorIdRetorna200() throws Exception {
        when(service.buscarPorId(15))
                .thenReturn(resposta());

        mvc.perform(get("/contratos/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNome").value("Empresa Alfa"));
    }

    @Test
    void atualizaContratoRetorna200() throws Exception {
        when(service.atualizar(eq(15), any(ContratoUpdateDTO.class)))
                .thenReturn(resposta());

        mvc.perform(put("/contratos/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projetoId": 3,
                                    "clienteNome": "Empresa Alfa",
                                    "valorTotal": 6000.00,
                                    "dataInicio": "2026-08-01",
                                    "dataFim": "2026-12-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void atualizaStatusRetorna200() throws Exception {
        when(service.atualizarStatus(
                eq(15), any(ContratoStatusUpdateDTO.class)
        )).thenReturn(
                new ContratoResponseDTO(
                        15, 3, "Empresa Alfa", null,
                        new BigDecimal("6000.00"),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 12, 1),
                        StatusContrato.ATIVO
                )
        );

        mvc.perform(patch("/contratos/15/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void excluiContratoRetorna204() throws Exception {
        doNothing().when(service).excluir(15);

        mvc.perform(delete("/contratos/15"))
                .andExpect(status().isNoContent());
    }

    @Test
    void jsonInvalidoRetorna400() throws Exception {
        mvc.perform(post("/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json-invalido"))
                .andExpect(status().isBadRequest());
    }
}
