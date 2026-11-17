package ds.FinHut8.contrato;

import ds.FinHut8.contrato.dtos.ContratoCreateDTO;
import ds.FinHut8.contrato.dtos.ContratoStatusUpdateDTO;
import ds.FinHut8.contrato.dtos.ContratoUpdateDTO;
import ds.FinHut8.contrato.enums.StatusContrato;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ContratoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 12, 1);
    private static final BigDecimal VALOR = new BigDecimal("6000.00");

    private final ContratoRepository repository = mock(ContratoRepository.class);
    private final ContratoService service = new ContratoService(repository);

    @Test
    void criaContratoComoRascunhoENormalizaNome() {
        when(repository.projetoExiste(3)).thenReturn(true);
        when(repository.inserir(any(Contrato.class))).thenAnswer(invocacao -> {
            Contrato recebido = invocacao.getArgument(0);
            return new Contrato(10, recebido.projetoId(), recebido.clienteNome(), recebido.arquivoRef(),
                    recebido.valorTotal(), recebido.dataInicio(), recebido.dataFim(), recebido.status());
        });

        var resposta = service.criar(new ContratoCreateDTO(3, " Empresa Alfa ", " ", VALOR, INICIO, FIM));

        assertEquals(10, resposta.id());
        assertEquals("Empresa Alfa", resposta.clienteNome());
        assertNull(resposta.arquivoRef());
        assertEquals(StatusContrato.RASCUNHO, resposta.status());
    }

    @Test
    void rejeitaProjetoInexistente() {
        when(repository.projetoExiste(99)).thenReturn(false);

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(99, "Cliente", null, VALOR, INICIO, FIM)));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(repository, never()).inserir(any());
    }

    @Test
    void rejeitaValorComMaisDeDuasCasasDecimais() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "Cliente", null,
                        new BigDecimal("10.001"), INICIO, FIM)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaPeriodoInvertido() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "Cliente", null, VALOR, FIM, INICIO)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void retorna404AoBuscarContratoInexistente() {
        when(repository.buscarPorId(10)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class, () -> service.buscarPorId(10));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void bloqueiaAtualizacaoQuandoExistemPrestacoes() {
        when(repository.buscarPorIdParaAtualizacao(10)).thenReturn(Optional.of(contrato(10, StatusContrato.ATIVO)));
        when(repository.projetoExiste(3)).thenReturn(true);
        when(repository.possuiPrestacoes(10)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.atualizar(10,
                new ContratoUpdateDTO(3, "Cliente", null, VALOR, INICIO, FIM)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).atualizar(any());
    }

    @Test
    void naoEncerraContratoComPrestacoesPendentes() {
        when(repository.buscarPorIdParaAtualizacao(10)).thenReturn(Optional.of(contrato(10, StatusContrato.ATIVO)));
        when(repository.possuiPrestacoesPendentes(10)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.atualizarStatus(10, new ContratoStatusUpdateDTO(StatusContrato.ENCERRADO)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).atualizarStatus(anyInt(), any());
    }

    @Test
    void naoPermiteReabrirContratoEncerrado() {
        when(repository.buscarPorIdParaAtualizacao(10)).thenReturn(Optional.of(contrato(10, StatusContrato.ENCERRADO)));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.atualizarStatus(10, new ContratoStatusUpdateDTO(StatusContrato.ATIVO)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).atualizarStatus(anyInt(), any());
    }

    @Test
    void bloqueiaExclusaoQuandoExistemPrestacoes() {
        when(repository.buscarPorIdParaAtualizacao(10)).thenReturn(Optional.of(contrato(10, StatusContrato.ATIVO)));
        when(repository.possuiPrestacoes(10)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.excluir(10));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).excluir(anyInt());
    }

    @Test
    void listaConverteEntidadesEmDtos() {
        when(repository.listar(3, "Empresa", StatusContrato.ATIVO))
                .thenReturn(List.of(contrato(10, StatusContrato.ATIVO)));

        var resultado = service.listar(3, " Empresa ", StatusContrato.ATIVO);

        assertEquals(1, resultado.size());
        assertEquals(10, resultado.getFirst().id());
        assertEquals("Cliente", resultado.getFirst().clienteNome());
    }

    @Test
    void rejeitaDtoNuloNaCriacao() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.criar(null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaNomeDeClienteAusente() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "   ", null, VALOR, INICIO, FIM)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaProjetoIdInvalido() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(0, "Cliente", null, VALOR, INICIO, FIM)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaValorNuloOuNaoPositivo() {
        var nulo = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "Cliente", null, null, INICIO, FIM)));
        var zero = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "Cliente", null, BigDecimal.ZERO, INICIO, FIM)));
        assertEquals(HttpStatus.BAD_REQUEST, nulo.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, zero.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaDatasObrigatoriasAusentes() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(new ContratoCreateDTO(3, "Cliente", null, VALOR, null, FIM)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void atualizaContratoSemPrestacoes() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.ATIVO)));
        when(repository.projetoExiste(3)).thenReturn(true);
        when(repository.possuiPrestacoes(10)).thenReturn(false);
        when(repository.atualizar(any(Contrato.class))).thenReturn(1);
        when(repository.buscarPorId(10)).thenAnswer(invocacao -> Optional.of(
                new Contrato(10, 3, "Cliente Atualizado", null, VALOR, INICIO, FIM, StatusContrato.ATIVO)));

        var resposta = service.atualizar(10,
                new ContratoUpdateDTO(3, "Cliente Atualizado", null, VALOR, INICIO, FIM));

        assertEquals("Cliente Atualizado", resposta.clienteNome());
        verify(repository).atualizar(argThat(c -> c.id().equals(10)
                && c.clienteNome().equals("Cliente Atualizado")
                && c.status() == StatusContrato.ATIVO));
    }

    @Test
    void excluiContratoSemPrestacoesQuandoAindaNaoTerminal() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.RASCUNHO)));
        when(repository.possuiPrestacoes(10)).thenReturn(false);
        when(repository.excluir(10)).thenReturn(1);

        assertDoesNotThrow(() -> service.excluir(10));
        verify(repository).excluir(10);
    }

    @Test
    void bloqueiaAtualizacaoDeContratoEncerrado() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.ENCERRADO)));

        var erro = assertThrows(ResponseStatusException.class, () -> service.atualizar(10,
                new ContratoUpdateDTO(3, "Cliente", null, VALOR, INICIO, FIM)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).atualizar(any());
    }

    @Test
    void bloqueiaExclusaoDeContratoRescindidoMesmoSemPrestacoes() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.RESCINDIDO)));

        var erro = assertThrows(ResponseStatusException.class, () -> service.excluir(10));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).possuiPrestacoes(anyInt());
        verify(repository, never()).excluir(anyInt());
    }

    @Test
    void retorna404AoExcluirContratoInexistente() {
        when(repository.buscarPorIdParaAtualizacao(999)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class, () -> service.excluir(999));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void rejeitaIdInvalidoNaConsulta() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.buscarPorId(0));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaIdInvalidoNaAtualizacaoDeStatus() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.atualizarStatus(-1, new ContratoStatusUpdateDTO(StatusContrato.ATIVO)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaStatusNulo() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.RASCUNHO)));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.atualizarStatus(10, new ContratoStatusUpdateDTO(null)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(repository, never()).atualizarStatus(anyInt(), any());
    }

    @Test
    void bloqueiaExclusaoQuandoRepositoryInformaPrestacoes() {
        when(repository.buscarPorIdParaAtualizacao(10))
                .thenReturn(Optional.of(contrato(10, StatusContrato.ATIVO)));
        when(repository.possuiPrestacoes(10)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.excluir(10));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).excluir(anyInt());
    }

    private Contrato contrato(Integer id, StatusContrato status) {
        return new Contrato(id, 3, "Cliente", null, VALOR, INICIO, FIM, status);
    }
}
