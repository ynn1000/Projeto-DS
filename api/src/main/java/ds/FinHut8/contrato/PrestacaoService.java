package ds.FinHut8.contrato;

import ds.FinHut8.contrato.dtos.PrestacaoCreateDTO;
import ds.FinHut8.contrato.dtos.PrestacaoResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PrestacaoService {
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999.99");
    private final ContratoRepository contratos;
    private final PrestacaoRepository prestacoes;

    public PrestacaoService(ContratoRepository contratos, PrestacaoRepository prestacoes) {
        this.contratos = contratos;
        this.prestacoes = prestacoes;
    }

    @Transactional(readOnly = true)
    public List<PrestacaoResponseDTO> listar(Integer contratoId) {
        validarId(contratoId, "contrato");
        if (contratos.buscarPorId(contratoId).isEmpty()) throw naoEncontrado("Contrato", contratoId);
        return prestacoes.listarPorContrato(contratoId).stream().map(PrestacaoResponseDTO::new).toList();
    }

    @Transactional
    public PrestacaoResponseDTO criar(Integer contratoId, PrestacaoCreateDTO dto) {
        validarId(contratoId, "contrato");
        Contrato contrato = contratos.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado("Contrato", contratoId));
        if (contrato.status() != ds.FinHut8.contrato.enums.StatusContrato.ATIVO) {
            throw erro(HttpStatus.CONFLICT, "Só é possível adicionar prestações a contratos ativos.");
        }
        validarDadosPrestacao(contrato, dto);
        BigDecimal comprometido = contratos.somarPrestacoesAtivas(contratoId);
        if (comprometido.add(dto.valor()).compareTo(contrato.valorTotal()) > 0) {
            throw erro(HttpStatus.CONFLICT, "A soma das prestações não pode ultrapassar o valor total do contrato. Saldo disponível: "
                    + contrato.valorTotal().subtract(comprometido) + ".");
        }
        try {
            return new PrestacaoResponseDTO(prestacoes.inserir(contratoId, dto.numero(), dto.valor(), dto.dataVencimento()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma prestação com esse número neste contrato ou os dados violam uma restrição.", e);
        }
    }

    @Transactional
    public PrestacaoResponseDTO atualizar(Integer contratoId, Integer prestacaoId, PrestacaoCreateDTO dto) {
        validarId(contratoId, "contrato");
        validarId(prestacaoId, "prestação");
        Contrato contrato = contratos.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado("Contrato", contratoId));
        Prestacao atual = prestacoes.buscarPorId(contratoId, prestacaoId)
                .orElseThrow(() -> naoEncontrado("Prestação", prestacaoId));
        if (atual.status() != StatusPrestacao.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Somente prestações pendentes podem ser editadas.");
        }
        if (prestacoes.possuiTransacao(prestacaoId) || prestacoes.possuiComprovante(prestacaoId)) {
            throw erro(HttpStatus.CONFLICT, "A prestação possui movimentação financeira ou comprovante vinculado e não pode ser editada.");
        }
        validarDadosPrestacao(contrato, dto);
        BigDecimal totalAtual = contratos.somarPrestacoesAtivas(contratoId);
        BigDecimal totalNovo = totalAtual.subtract(atual.valor()).add(dto.valor());
        if (totalNovo.compareTo(contrato.valorTotal()) > 0) {
            throw erro(HttpStatus.CONFLICT, "A soma das prestações não pode ultrapassar o valor total do contrato. Saldo disponível para esta alteração: "
                    + contrato.valorTotal().subtract(totalAtual.subtract(atual.valor())) + ".");
        }
        try {
            if (prestacoes.atualizarPendente(contratoId, prestacaoId, dto.numero(), dto.valor(), dto.dataVencimento()) == 0) {
                throw erro(HttpStatus.CONFLICT, "A prestação deixou de estar pendente e não pode ser editada.");
            }
            return prestacoes.buscarPorId(contratoId, prestacaoId)
                    .map(PrestacaoResponseDTO::new)
                    .orElseThrow(() -> naoEncontrado("Prestação", prestacaoId));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma prestação com esse número neste contrato ou os dados violam uma restrição.", e);
        }
    }

    private void validarDadosPrestacao(Contrato contrato, PrestacaoCreateDTO dto) {
        if (dto == null || dto.numero() == null || dto.numero() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um número de prestação positivo.");
        }
        if (dto.valor() == null || dto.valor().signum() <= 0 || dto.valor().compareTo(VALOR_MAXIMO) > 0
                || dto.valor().stripTrailingZeros().scale() > 2) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um valor positivo com no máximo duas casas decimais.");
        }
        if (dto.dataVencimento() == null || dto.dataVencimento().isBefore(contrato.dataInicio())
                || dto.dataVencimento().isAfter(contrato.dataFim())) {
            throw erro(HttpStatus.BAD_REQUEST, "O vencimento deve estar dentro do período do contrato.");
        }
    }

    @Transactional
    public void cancelar(Integer contratoId, Integer prestacaoId) {
        validarId(contratoId, "contrato");
        validarId(prestacaoId, "prestação");
        contratos.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado("Contrato", contratoId));
        Prestacao prestacao = prestacoes.buscarPorId(contratoId, prestacaoId)
                .orElseThrow(() -> naoEncontrado("Prestação", prestacaoId));
        if (prestacao.status() != StatusPrestacao.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Somente prestações pendentes podem ser canceladas.");
        }
        if (prestacoes.possuiTransacao(prestacaoId) || prestacoes.possuiComprovante(prestacaoId)) {
            throw erro(HttpStatus.CONFLICT, "A prestação possui movimentação financeira ou comprovante vinculado e não pode ser cancelada.");
        }
        if (prestacoes.cancelar(contratoId, prestacaoId) == 0) {
            throw erro(HttpStatus.CONFLICT, "A prestação deixou de estar pendente e não pode ser cancelada.");
        }
    }

    private void validarId(Integer id, String nome) {
        if (id == null || id <= 0) throw erro(HttpStatus.BAD_REQUEST, "Informe um ID de " + nome + " positivo.");
    }
    private ResponseStatusException naoEncontrado(String nome, Integer id) {
        return erro(HttpStatus.NOT_FOUND, nome + " " + id + " não encontrado(a).");
    }
    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
