package ds.FinHut8.transacao;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import ds.FinHut8.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransacaoServiceTest {
    private final TransacaoRepository repository = mock(TransacaoRepository.class);
    private final CaixaRepository caixaRepository = mock(CaixaRepository.class);
    private final TransacaoService service = new TransacaoService(repository, caixaRepository);
    private final User usuario = new User();
    private final LocalDate data = LocalDate.of(2026, 11, 14);
    private final BigDecimal valor = new BigDecimal("50.00");

    @BeforeEach
    void configurar() {
        usuario.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(caixaRepository.find()).thenReturn(Optional.of(new Caixa(1, new BigDecimal("1000.00"),
                new BigDecimal("1500.00"), new BigDecimal("1000.00"))));
        when(repository.funcionarioIdPorUsuario(usuario.getId())).thenReturn(9);
    }

    @Test
    void registraDespesaAssociandoFuncionarioAutenticado() {
        when(repository.categoriaOrcamentariaAtivaDoCaixa(1, 3)).thenReturn(true);
        when(repository.inserir(1, 9, "Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS,
                valor, data, 3, null, null, null)).thenReturn(transacao(1, 3, null, null));

        var resultado = service.registrar(dto(TipoTransacao.DESPESA, 3, null, null, null), usuario);

        assertEquals(1, resultado.id());
        assertEquals(9, resultado.registradoPorId());
        verify(repository).inserir(1, 9, "Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS,
                valor, data, 3, null, null, null);
    }

    @Test
    void receitaComPrestacaoValidaVinculaProjetoELiquidaPrestacao() {
        when(repository.buscarPrestacaoParaPagamento(7)).thenReturn(
                new TransacaoRepository.PrestacaoVinculo(7, valor, "PENDENTE", 22, 4));
        when(repository.projetoPertenceAoCaixa(1, 22)).thenReturn(true);
        when(repository.inserir(1, 9, "Recebimento parcela", TipoTransacao.RECEITA, CategoriaTransacao.VENDA,
                valor, data, null, null, 22, 7)).thenReturn(transacao(2, null, 22, 7));
        when(repository.marcarPrestacaoPaga(7)).thenReturn(1);

        var resultado = service.registrar(dto(TipoTransacao.RECEITA, null, null, 22, 7), usuario);

        assertEquals(22, resultado.projetoId());
        assertEquals(7, resultado.prestacaoId());
        verify(repository).marcarPrestacaoPaga(7);
    }

    @Test
    void rejeitaPrestacaoJaPaga() {
        when(repository.buscarPrestacaoParaPagamento(7)).thenReturn(
                new TransacaoRepository.PrestacaoVinculo(7, valor, "PAGA", 22, 4));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(dto(TipoTransacao.RECEITA, null, null, 22, 7), usuario));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repository, never()).inserir(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejeitaProjetoDeOutraEmpresa() {
        when(repository.projetoPertenceAoCaixa(1, 22)).thenReturn(false);
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(dto(TipoTransacao.RECEITA, null, null, 22, null), usuario));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(repository, never()).inserir(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejeitaFuncionarioSemVinculo() {
        when(repository.funcionarioIdPorUsuario(usuario.getId())).thenThrow(new IllegalStateException());
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(dto(TipoTransacao.RECEITA, null, null, null, null), usuario));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
    }

    @Test
    void rejeitaDespesaSemCategoria() {
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(dto(TipoTransacao.DESPESA, null, null, null, null), usuario));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    private TransacaoCreateDTO dto(TipoTransacao tipo, Integer categoria, Integer subverba,
                                   Integer projeto, Integer prestacao) {
        return new TransacaoCreateDTO("Compra", tipo,
                tipo == TipoTransacao.RECEITA ? CategoriaTransacao.VENDA : CategoriaTransacao.ALIMENTOS,
                valor, data, categoria, subverba, projeto, prestacao);
    }

    private Transacao transacao(Integer id, Integer categoria, Integer projeto, Integer prestacao) {
        return new Transacao(id, "Compra", TipoTransacao.RECEITA, CategoriaTransacao.VENDA, valor, data,
                categoria, null, projeto, prestacao, 9, OffsetDateTime.now());
    }
}
