package ds.FinHut8.transacao;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransacaoServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 11, 14);
    private static final OffsetDateTime CRIADO_EM = DATA.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    private static final BigDecimal VALOR = new BigDecimal("50.00");

    private final TransacaoRepository repository = mock(TransacaoRepository.class);
    private final CaixaRepository caixaRepository = mock(CaixaRepository.class);
    private final TransacaoService service = new TransacaoService(repository, caixaRepository);

    @BeforeEach
    void configurarCaixa() {
        when(caixaRepository.find()).thenReturn(Optional.of(new Caixa(
                1, new BigDecimal("1000.00"), new BigDecimal("1500.00"), new BigDecimal("500.00"))));
    }

    @Test
    void registraDespesaComCategoriaOrcamentariaAtiva() {
        when(repository.categoriaOrcamentariaAtivaDoCaixa(1, 3)).thenReturn(true);
        when(repository.inserir(1, "Compra de material", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, null))
                .thenReturn(new Transacao(5, "Compra de material", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, null, CRIADO_EM));

        var resultado = service.registrar(new TransacaoCreateDTO(
                "  Compra de material  ", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, null));

        assertEquals(5, resultado.id());
        assertEquals("Compra de material", resultado.descricao());
        assertEquals(TipoTransacao.DESPESA, resultado.tipo());
        assertEquals(CategoriaTransacao.ALIMENTOS, resultado.categoriaTransacao());
        assertEquals(3, resultado.categoriaOrcamentariaId());
        assertEquals(VALOR, resultado.valor());
    }

    @Test
    void registraReceitaSemCategoriaOrcamentaria() {
        when(repository.inserir(1, "Recebimento", TipoTransacao.RECEITA, CategoriaTransacao.VENDA, VALOR, DATA, null, null))
                .thenReturn(new Transacao(6, "Recebimento", TipoTransacao.RECEITA, CategoriaTransacao.VENDA, VALOR, DATA, null, null, CRIADO_EM));

        var resultado = service.registrar(new TransacaoCreateDTO(
                "Recebimento", TipoTransacao.RECEITA, CategoriaTransacao.VENDA, VALOR, DATA, null, null));

        assertEquals(TipoTransacao.RECEITA, resultado.tipo());
        assertEquals(CategoriaTransacao.VENDA, resultado.categoriaTransacao());
        assertNull(resultado.categoriaOrcamentariaId());
        verify(repository).inserir(1, "Recebimento", TipoTransacao.RECEITA, CategoriaTransacao.VENDA, VALOR, DATA, null, null);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejeitaDespesaSemCategoriaOrcamentaria() {
        rejeita(new TransacaoCreateDTO("Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaReceitaComCategoriaOrcamentaria() {
        rejeita(new TransacaoCreateDTO("Venda", TipoTransacao.RECEITA,
                CategoriaTransacao.VENDA, VALOR, DATA, 3, null));
        verifyNoInteractions(repository, caixaRepository);
    }

    @Test
    void rejeitaLancamentoSemCategoriaDaTransacao() {
        rejeita(new TransacaoCreateDTO("Compra", TipoTransacao.DESPESA, null, VALOR, DATA, 3, null));
        verifyNoInteractions(repository, caixaRepository);
    }

    @Test
    void rejeitaCategoriaInvalidaSemInserir() {
        rejeita(new TransacaoCreateDTO("Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, null));
        verify(repository).categoriaOrcamentariaAtivaDoCaixa(1, 3);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void registraDespesaVinculadaASubverba() {
        when(repository.categoriaOrcamentariaAtivaDoCaixa(1, 3)).thenReturn(true);
        when(repository.subverbaValida(1, 3, 7, DATA)).thenReturn(true);
        when(repository.inserir(1, "Lanches", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS,
                VALOR, DATA, 3, 7)).thenReturn(new Transacao(5, "Lanches", TipoTransacao.DESPESA,
                CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, 7, CRIADO_EM));

        var resultado = service.registrar(new TransacaoCreateDTO("Lanches", TipoTransacao.DESPESA,
                CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, 7));

        assertEquals(3, resultado.categoriaOrcamentariaId());
        assertEquals(7, resultado.subverbaId());
    }

    @Test
    void rejeitaSubverbaQueNaoCorrespondeAoLancamento() {
        when(repository.categoriaOrcamentariaAtivaDoCaixa(1, 3)).thenReturn(true);

        rejeita(new TransacaoCreateDTO("Lanches", TipoTransacao.DESPESA,
                CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, 7));

        verify(repository).categoriaOrcamentariaAtivaDoCaixa(1, 3);
        verify(repository).subverbaValida(1, 3, 7, DATA);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejeitaReceitaComSubverba() {
        rejeita(new TransacaoCreateDTO("Venda", TipoTransacao.RECEITA,
                CategoriaTransacao.VENDA, VALOR, DATA, null, 7));

        verifyNoInteractions(repository, caixaRepository);
    }

    @Test
    void rejeitaValoresInvalidos() {
        for (String valor : List.of("0", "-1", "50.001", "10000000.00")) {
            rejeita(new TransacaoCreateDTO("Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, new BigDecimal(valor), DATA, 3, null));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void listaSomenteTransacoesDoCaixa() {
        when(repository.listar(1)).thenReturn(List.of(
                new Transacao(5, "Compra", TipoTransacao.DESPESA, CategoriaTransacao.ALIMENTOS, VALOR, DATA, 3, null, CRIADO_EM)));

        var resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals(5, resultado.getFirst().id());
        assertEquals(CategoriaTransacao.ALIMENTOS, resultado.getFirst().categoriaTransacao());
        assertEquals(3, resultado.getFirst().categoriaOrcamentariaId());
        verify(repository).listar(1);
    }

    @Test
    void retorna404SemCaixa() {
        when(caixaRepository.find()).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class, service::listar);

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verifyNoInteractions(repository);
    }

    private void rejeita(TransacaoCreateDTO dto) {
        var erro = assertThrows(ResponseStatusException.class, () -> service.registrar(dto));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }
}
