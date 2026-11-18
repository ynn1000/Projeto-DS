package ds.FinHut8.transacao;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import ds.FinHut8.transacao.dtos.ComprovanteCreateDTO;
import ds.FinHut8.transacao.dtos.ComprovanteResponseDTO;
import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import ds.FinHut8.transacao.dtos.TransacaoResponseDTO;
import ds.FinHut8.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoService {
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999.99");
    private final TransacaoRepository repository;
    private final CaixaRepository caixaRepository;

    public TransacaoService(TransacaoRepository repository, CaixaRepository caixaRepository) {
        this.repository = repository; this.caixaRepository = caixaRepository;
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponseDTO> listar(TipoTransacao tipo, CategoriaTransacao categoriaTransacao,
            Integer categoriaId, Integer projetoId, LocalDate inicio, LocalDate fim) {
        validarFiltros(categoriaId, projetoId, inicio, fim);
        return repository.listar(caixaId(), tipo, categoriaTransacao, categoriaId, projetoId, inicio, fim)
                .stream().map(TransacaoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetoOpcaoDTO> listarProjetosAtivos() { return repository.listarProjetosAtivos(caixaId()); }

    @Transactional(readOnly = true)
    public List<PrestacaoOpcaoDTO> listarPrestacoesPendentes() { return repository.listarPrestacoesPendentes(caixaId()); }

    @Transactional(readOnly = true)
    public List<PeriodoOrcamentoOpcaoDTO> listarPeriodosOrcamentoAtivos() { return repository.listarPeriodosAtivos(caixaId()); }

    @Transactional(readOnly = true)
    public TransacaoResumo resumir(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um período válido: dataInicio e dataFim.");
        }
        return repository.resumir(caixaId(), inicio, fim);
    }

    @Transactional
    public TransacaoResponseDTO registrar(TransacaoCreateDTO dto, User usuario) {
        Dados dados = validar(dto);
        if (usuario == null || usuario.getId() == null) {
            throw erro(HttpStatus.UNAUTHORIZED, "Não foi possível identificar o usuário autenticado.");
        }
        Integer caixaId = caixaId();
        Integer funcionarioId;
        try {
            funcionarioId = repository.funcionarioIdPorUsuario(usuario.getId());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O usuário autenticado não possui um funcionário ativo vinculado.", e);
        }

        Integer projetoId = dados.projetoId();
        Integer prestacaoId = dados.prestacaoId();
        if (prestacaoId != null) {
            if (dados.tipo() != TipoTransacao.RECEITA) {
                throw erro(HttpStatus.BAD_REQUEST, "Uma prestação de contrato só pode ser vinculada a uma receita.");
            }
            TransacaoRepository.PrestacaoVinculo prestacao = repository.buscarPrestacaoParaPagamento(prestacaoId);
            if (prestacao == null) throw erro(HttpStatus.NOT_FOUND, "Prestação não encontrada.");
            if (!"PENDENTE".equals(prestacao.status())) {
                throw erro(HttpStatus.CONFLICT, "A prestação não está pendente e não pode receber outro pagamento.");
            }
            if (dados.valor().compareTo(prestacao.valor()) != 0) {
                throw erro(HttpStatus.BAD_REQUEST, "O valor da receita deve corresponder ao valor integral da prestação.");
            }
            if (projetoId != null && !projetoId.equals(prestacao.projetoId())) {
                throw erro(HttpStatus.BAD_REQUEST, "O projeto informado não corresponde ao projeto do contrato da prestação.");
            }
            projetoId = prestacao.projetoId();
        }
        if (projetoId != null && !repository.projetoPertenceAoCaixa(caixaId, projetoId)) {
            throw erro(HttpStatus.BAD_REQUEST, "O projeto informado não pertence à empresa do caixa.");
        }
        if (dados.categoriaId() != null && !repository.categoriaOrcamentariaAtivaDoCaixa(caixaId, dados.categoriaId())) {
            throw erro(HttpStatus.BAD_REQUEST, "A categoria orçamentária deve estar ativa e pertencer à empresa do caixa.");
        }
        if (dados.subverbaId() != null && !repository.subverbaValida(
                caixaId, dados.categoriaId(), dados.subverbaId(), dados.data())) {
            throw erro(HttpStatus.BAD_REQUEST, "A subverba precisa pertencer à verba e a um período ativo que contenha a data do lançamento.");
        }
        try {
            Transacao criada = repository.inserir(caixaId, funcionarioId, dados.descricao(), dados.tipo(),
                    dados.categoriaTransacao(), dados.valor(), dados.data(), dados.categoriaId(),
                    dados.subverbaId(), projetoId, prestacaoId);
            if (prestacaoId != null && repository.marcarPrestacaoPaga(prestacaoId) != 1) {
                throw erro(HttpStatus.CONFLICT, "A prestação foi alterada durante o registro. Atualize os dados e tente novamente.");
            }
            return new TransacaoResponseDTO(criada);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível registrar a transação devido a um vínculo financeiro duplicado ou inválido.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ComprovanteResponseDTO> listarComprovantes(Integer transacaoId) {
        validarId(transacaoId);
        Integer caixaId = caixaId();
        if (!repository.transacaoExisteNoCaixa(caixaId, transacaoId)) throw naoEncontrado(transacaoId);
        return repository.listarComprovantes(caixaId, transacaoId).stream().map(ComprovanteResponseDTO::new).toList();
    }

    @Transactional
    public ComprovanteResponseDTO adicionarComprovante(Integer transacaoId, ComprovanteCreateDTO dto) {
        validarId(transacaoId);
        Integer caixaId = caixaId();
        if (!repository.transacaoExisteNoCaixa(caixaId, transacaoId)) throw naoEncontrado(transacaoId);
        validarComprovante(dto);
        String arquivoRef = dto.arquivoRef().strip();
        String numero = dto.numeroNotaFiscal() == null || dto.numeroNotaFiscal().isBlank()
                ? null : dto.numeroNotaFiscal().strip();
        return new ComprovanteResponseDTO(repository.inserirComprovante(transacaoId, dto.tipo(), arquivoRef,
                numero, dto.valor(), dto.dataDocumento()));
    }

    @Transactional(readOnly = true)
    public List<ComprovanteResponseDTO> listarComprovantesPrestacao(Integer prestacaoId) {
        validarId(prestacaoId);
        Integer caixaId = caixaId();
        if (!repository.prestacaoExisteNoCaixa(caixaId, prestacaoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestação não encontrada.");
        }
        return repository.listarComprovantesPrestacao(caixaId, prestacaoId)
                .stream().map(ComprovanteResponseDTO::new).toList();
    }

    @Transactional
    public ComprovanteResponseDTO adicionarComprovantePrestacao(Integer prestacaoId, ComprovanteCreateDTO dto) {
        validarId(prestacaoId);
        Integer caixaId = caixaId();
        if (!repository.prestacaoExisteNoCaixa(caixaId, prestacaoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestação não encontrada.");
        }
        validarComprovante(dto);
        String numero = dto.numeroNotaFiscal() == null || dto.numeroNotaFiscal().isBlank()
                ? null : dto.numeroNotaFiscal().strip();
        return new ComprovanteResponseDTO(repository.inserirComprovantePrestacao(prestacaoId, dto.tipo(),
                dto.arquivoRef().strip(), numero, dto.valor(), dto.dataDocumento()));
    }

    private void validarComprovante(ComprovanteCreateDTO dto) {
        if (dto == null || dto.tipo() == null || dto.arquivoRef() == null || dto.arquivoRef().isBlank()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o tipo e a referência do arquivo do comprovante.");
        }
        if (dto.arquivoRef().strip().length() > 4000) throw erro(HttpStatus.BAD_REQUEST, "A referência do arquivo deve ter até 4000 caracteres.");
        if (dto.valor() != null && (dto.valor().signum() <= 0 || dto.valor().compareTo(VALOR_MAXIMO) > 0
                || dto.valor().stripTrailingZeros().scale() > 2)) {
            throw erro(HttpStatus.BAD_REQUEST, "O valor do comprovante deve ser positivo e ter no máximo duas casas decimais.");
        }
        if (dto.numeroNotaFiscal() != null && dto.numeroNotaFiscal().strip().length() > 60) {
            throw erro(HttpStatus.BAD_REQUEST, "O número da nota fiscal deve ter até 60 caracteres.");
        }
    }

    private Dados validar(TransacaoCreateDTO dto) {
        if (dto == null || dto.descricao() == null || dto.descricao().isBlank()) throw erro(HttpStatus.BAD_REQUEST, "Informe a descrição.");
        String descricao = dto.descricao().strip();
        if (descricao.codePointCount(0, descricao.length()) > 255) throw erro(HttpStatus.BAD_REQUEST, "A descrição deve ter no máximo 255 caracteres.");
        if (dto.tipo() == null || dto.dataMovimento() == null) throw erro(HttpStatus.BAD_REQUEST, "Informe o tipo e a data do movimento.");
        if (dto.categoriaTransacao() == null) throw erro(HttpStatus.BAD_REQUEST, "Informe a categoria da transação.");
        BigDecimal valor = dto.valor();
        if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAXIMO) > 0 || valor.stripTrailingZeros().scale() > 2)
            throw erro(HttpStatus.BAD_REQUEST, "Informe um valor maior que zero, até 9999999,99 e com no máximo duas casas decimais.");
        Integer categoriaId = dto.categoriaOrcamentariaId();
        Integer subverbaId = dto.subverbaId();
        if (dto.tipo() == TipoTransacao.DESPESA && categoriaId == null) throw erro(HttpStatus.BAD_REQUEST, "Informe a categoria orçamentária da despesa.");
        if (dto.tipo() == TipoTransacao.RECEITA && (categoriaId != null || subverbaId != null)) throw erro(HttpStatus.BAD_REQUEST, "Receitas não usam verba orçamentária nem subverba.");
        if (categoriaId != null && categoriaId <= 0) throw erro(HttpStatus.BAD_REQUEST, "Informe um ID de categoria orçamentária positivo.");
        if (subverbaId != null && subverbaId <= 0) throw erro(HttpStatus.BAD_REQUEST, "Informe um ID de subverba positivo.");
        if (dto.projetoId() != null && dto.projetoId() <= 0) throw erro(HttpStatus.BAD_REQUEST, "O ID do projeto deve ser positivo.");
        if (dto.prestacaoId() != null && dto.prestacaoId() <= 0) throw erro(HttpStatus.BAD_REQUEST, "O ID da prestação deve ser positivo.");
        if (dto.prestacaoId() != null && dto.tipo() != TipoTransacao.RECEITA) throw erro(HttpStatus.BAD_REQUEST, "Prestação só pode ser vinculada a receita.");
        return new Dados(descricao, dto.tipo(), dto.categoriaTransacao(), valor, dto.dataMovimento(), categoriaId, subverbaId, dto.projetoId(), dto.prestacaoId());
    }

    private void validarFiltros(Integer categoriaId, Integer projetoId, LocalDate inicio, LocalDate fim) {
        if (categoriaId != null && categoriaId <= 0) throw erro(HttpStatus.BAD_REQUEST, "O ID da categoria deve ser positivo.");
        if (projetoId != null && projetoId <= 0) throw erro(HttpStatus.BAD_REQUEST, "O ID do projeto deve ser positivo.");
        if ((inicio == null) != (fim == null)) throw erro(HttpStatus.BAD_REQUEST, "Informe dataInicio e dataFim juntas.");
        if (inicio != null && fim.isBefore(inicio)) throw erro(HttpStatus.BAD_REQUEST, "dataFim não pode ser anterior a dataInicio.");
    }
    private Integer caixaId() { return caixaRepository.find().map(Caixa::id)
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Caixa não encontrado.")); }
    private void validarId(Integer id) { if (id == null || id <= 0) throw erro(HttpStatus.BAD_REQUEST, "O ID deve ser positivo."); }
    private ResponseStatusException naoEncontrado(Integer id) { return erro(HttpStatus.NOT_FOUND, "Transação " + id + " não encontrada."); }
    private ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }
    private record Dados(String descricao, TipoTransacao tipo, CategoriaTransacao categoriaTransacao, BigDecimal valor,
            LocalDate data, Integer categoriaId, Integer subverbaId, Integer projetoId, Integer prestacaoId) {}
}
