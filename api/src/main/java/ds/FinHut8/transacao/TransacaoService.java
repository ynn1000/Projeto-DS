package ds.FinHut8.transacao;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import ds.FinHut8.transacao.dtos.TransacaoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransacaoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999.99");

    private final TransacaoRepository repository;
    private final CaixaRepository caixaRepository;

    public TransacaoService(TransacaoRepository repository, CaixaRepository caixaRepository) {
        this.repository = repository;
        this.caixaRepository = caixaRepository;
    }

    public List<TransacaoResponseDTO> listar() {
        return repository.listar(buscarCaixaId()).stream()
                .map(TransacaoResponseDTO::new)
                .toList();
    }

    public TransacaoResponseDTO registrar(TransacaoCreateDTO dto) {
        if (dto == null || dto.descricao() == null || dto.descricao().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a descrição.");
        }
        String descricao = dto.descricao().strip();
        if (descricao.codePointCount(0, descricao.length()) > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A descrição deve ter no máximo 255 caracteres.");
        }
        if (dto.tipo() == null || dto.dataMovimento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o tipo e a data do movimento.");
        }

        if (dto.categoriaTransacao() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a categoria da transação.");
        }

        BigDecimal valor = dto.valor();
        if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAXIMO) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um valor maior que zero, até 9999999,99 e com no máximo duas casas decimais.");
        }

        Integer categoriaOrcamentariaId = dto.categoriaOrcamentariaId();
        Integer subverbaId = dto.subverbaId();
        if (dto.tipo() == TipoTransacao.DESPESA && categoriaOrcamentariaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a categoria orçamentária da despesa.");
        }
        if (dto.tipo() == TipoTransacao.RECEITA && (categoriaOrcamentariaId != null || subverbaId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receitas não usam verba orçamentária nem subverba.");
        }
        if (categoriaOrcamentariaId != null && categoriaOrcamentariaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um ID de categoria orçamentária positivo.");
        }

        if (subverbaId != null && subverbaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um ID de subverba positivo.");
        }

        Integer caixaId = buscarCaixaId();
        if (categoriaOrcamentariaId != null && !repository.categoriaOrcamentariaAtivaDoCaixa(caixaId, categoriaOrcamentariaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A categoria orçamentária deve estar ativa e pertencer à empresa do caixa.");
        }

        if (subverbaId != null && !repository.subverbaValida(
                caixaId, categoriaOrcamentariaId, subverbaId, dto.dataMovimento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A subverba precisa pertencer à verba e ao período do lançamento.");
        }

        Transacao transacao = repository.inserir(
                caixaId, descricao, dto.tipo(), dto.categoriaTransacao(), valor,
                dto.dataMovimento(), categoriaOrcamentariaId, subverbaId);
        return new TransacaoResponseDTO(transacao);
    }

    private Integer buscarCaixaId() {
        return caixaRepository.find()
                .map(Caixa::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caixa não encontrado."));
    }
}
