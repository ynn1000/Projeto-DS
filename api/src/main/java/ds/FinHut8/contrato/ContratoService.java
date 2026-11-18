package ds.FinHut8.contrato;

import ds.FinHut8.contrato.dtos.ContratoCreateDTO;
import ds.FinHut8.contrato.dtos.ContratoResponseDTO;
import ds.FinHut8.contrato.dtos.ContratoStatusUpdateDTO;
import ds.FinHut8.contrato.dtos.ContratoUpdateDTO;
import ds.FinHut8.contrato.enums.StatusContrato;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContratoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999.99");
    private static final int CLIENTE_NOME_MAXIMO = 35;

    private final ContratoRepository repository;

    public ContratoService(ContratoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listar(Integer projetoId, String clienteNome, StatusContrato status) {
        if (projetoId != null && projetoId <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O ID do projeto deve ser positivo.");
        }
        String filtroCliente = normalizarFiltro(clienteNome);
        if (filtroCliente != null && filtroCliente.codePointCount(0, filtroCliente.length()) > CLIENTE_NOME_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "O filtro de cliente deve ter até 35 caracteres.");
        }
        return repository.listar(projetoId, filtroCliente, status).stream()
                .map(ContratoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Integer id) {
        validarId(id);
        return repository.buscarPorId(id)
                .map(ContratoResponseDTO::new)
                .orElseThrow(() -> naoEncontrado(id));
    }

    @Transactional(readOnly = true)
    public List<PrestacaoContratoDTO> listarPrestacoes(Integer contratoId) {
        validarId(contratoId);
        if (repository.buscarPorId(contratoId).isEmpty()) throw naoEncontrado(contratoId);
        return repository.listarPrestacoes(contratoId);
    }

    @Transactional
    public PrestacaoContratoDTO criarPrestacao(Integer contratoId, ds.FinHut8.contrato.dtos.PrestacaoCreateDTO dto) {
        validarId(contratoId);
        Contrato contrato = repository.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado(contratoId));
        if (contrato.status() != StatusContrato.ATIVO) {
            throw erro(HttpStatus.CONFLICT, "Só é possível adicionar prestações a contratos ativos.");
        }
        validarDadosPrestacao(contrato, dto);
        BigDecimal comprometido = repository.somarPrestacoesAtivas(contratoId);
        if (comprometido.add(dto.valor()).compareTo(contrato.valorTotal()) > 0) {
            throw erro(HttpStatus.CONFLICT, "A soma das prestações não pode ultrapassar o valor total do contrato.");
        }
        try {
            return repository.inserirPrestacao(contratoId, dto.numero(), dto.valor(), dto.dataVencimento());
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("Já existe uma prestação com esse número neste contrato ou os dados violam uma restrição do banco.", e);
        }
    }

    @Transactional
    public PrestacaoContratoDTO atualizarPrestacao(Integer contratoId, Integer prestacaoId,
                                                    ds.FinHut8.contrato.dtos.PrestacaoCreateDTO dto) {
        validarId(contratoId);
        validarId(prestacaoId);
        Contrato contrato = repository.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> naoEncontrado(contratoId));
        if (contrato.status() != StatusContrato.ATIVO) {
            throw erro(HttpStatus.CONFLICT, "Só é possível editar prestações de contratos ativos.");
        }
        PrestacaoContratoDTO atual = repository.listarPrestacoes(contratoId).stream()
                .filter(p -> p.id().equals(prestacaoId)).findFirst()
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Prestação não encontrada neste contrato."));
        if (!"PENDENTE".equals(atual.status())) {
            throw erro(HttpStatus.CONFLICT, "Somente prestações pendentes podem ser editadas.");
        }
        if (repository.prestacaoPossuiVinculos(prestacaoId)) {
            throw erro(HttpStatus.CONFLICT, "Não é possível editar uma prestação vinculada a transação ou comprovante.");
        }
        validarDadosPrestacao(contrato, dto);
        BigDecimal comprometido = repository.somarPrestacoesAtivas(contratoId)
                .subtract(atual.valor()).add(dto.valor());
        if (comprometido.compareTo(contrato.valorTotal()) > 0) {
            throw erro(HttpStatus.CONFLICT, "A soma das prestações não pode ultrapassar o valor total do contrato.");
        }
        try {
            if (repository.atualizarPrestacao(contratoId, prestacaoId, dto.numero(), dto.valor(), dto.dataVencimento()) != 1) {
                throw erro(HttpStatus.CONFLICT, "A prestação foi alterada ou não está mais pendente.");
            }
            return repository.listarPrestacoes(contratoId).stream()
                    .filter(p -> p.id().equals(prestacaoId)).findFirst().orElseThrow();
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("Já existe uma prestação com esse número neste contrato ou os dados violam uma restrição do banco.", e);
        }
    }

    @Transactional
    public void cancelarPrestacao(Integer contratoId, Integer prestacaoId) {
        validarId(contratoId); validarId(prestacaoId);
        if (repository.buscarPorIdParaAtualizacao(contratoId).isEmpty()) throw naoEncontrado(contratoId);
        if (repository.prestacaoPossuiVinculos(prestacaoId)) {
            throw erro(HttpStatus.CONFLICT, "Não é possível cancelar uma prestação vinculada a transação ou comprovante.");
        }
        if (repository.cancelarPrestacao(contratoId, prestacaoId) != 1) {
            throw erro(HttpStatus.CONFLICT, "A prestação não existe neste contrato ou não está pendente.");
        }
    }

    @Transactional
    public ContratoResponseDTO criar(ContratoCreateDTO dto) {
        DadosContrato dados = validarDados(dto == null ? null : dto.projetoId(),
                dto == null ? null : dto.clienteNome(), dto == null ? null : dto.arquivoRef(),
                dto == null ? null : dto.valorTotal(), dto == null ? null : dto.dataInicio(),
                dto == null ? null : dto.dataFim());

        validarProjeto(dados.projetoId());

        Contrato novo = new Contrato(null, dados.projetoId(), dados.clienteNome(), dados.arquivoRef(),
                dados.valorTotal(), dados.dataInicio(), dados.dataFim(), StatusContrato.RASCUNHO);
        try {
            return new ContratoResponseDTO(repository.inserir(novo));
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("Não foi possível cadastrar o contrato porque algum relacionamento ou dado foi alterado durante a operação.", e);
        }
    }

    @Transactional
    public ContratoResponseDTO atualizar(Integer id, ContratoUpdateDTO dto) {
        validarId(id);
        Contrato atual = repository.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> naoEncontrado(id));

        if (atual.status() == StatusContrato.ENCERRADO || atual.status() == StatusContrato.RESCINDIDO) {
            throw erro(HttpStatus.CONFLICT,
                    "Contratos encerrados ou rescindidos não podem ser alterados.");
        }

        DadosContrato dados = validarDados(dto == null ? null : dto.projetoId(),
                dto == null ? null : dto.clienteNome(), dto == null ? null : dto.arquivoRef(),
                dto == null ? null : dto.valorTotal(), dto == null ? null : dto.dataInicio(),
                dto == null ? null : dto.dataFim());
        validarProjeto(dados.projetoId());

        if (repository.possuiPrestacoes(id)) {
            if (!atual.projetoId().equals(dados.projetoId())) {
                throw erro(HttpStatus.CONFLICT,
                        "Não é possível trocar o projeto de um contrato que possui prestações vinculadas.");
            }
            if (repository.somarPrestacoesAtivas(id).compareTo(dados.valorTotal()) > 0) {
                throw erro(HttpStatus.CONFLICT,
                        "O valor total do contrato não pode ser menor que a soma das prestações não canceladas.");
            }
            if (!repository.prestacoesDentroDoPeriodo(id, dados.dataInicio(), dados.dataFim())) {
                throw erro(HttpStatus.CONFLICT,
                        "O período do contrato deve abranger os vencimentos de todas as prestações não canceladas.");
            }
        }

        Contrato atualizado = new Contrato(atual.id(), dados.projetoId(), dados.clienteNome(),
                dados.arquivoRef(), dados.valorTotal(), dados.dataInicio(), dados.dataFim(), atual.status());
        try {
            if (repository.atualizar(atualizado) == 0) {
                throw naoEncontrado(id);
            }
            return repository.buscarPorId(id)
                    .map(ContratoResponseDTO::new)
                    .orElseThrow(() -> naoEncontrado(id));
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("A atualização viola uma restrição de integridade do banco de dados.", e);
        }
    }

    @Transactional
    public ContratoResponseDTO atualizarStatus(Integer id, ContratoStatusUpdateDTO dto) {
        validarId(id);
        if (dto == null || dto.status() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o novo status do contrato.");
        }

        Contrato atual = repository.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> naoEncontrado(id));
        StatusContrato destino = dto.status();
        validarTransicao(atual.status(), destino);

        if (destino == StatusContrato.ENCERRADO && repository.possuiPrestacoesPendentes(id)) {
            throw erro(HttpStatus.CONFLICT,
                    "O contrato possui prestações pendentes e não pode ser encerrado.");
        }

        try {
            if (repository.atualizarStatus(id, destino) == 0) {
                throw naoEncontrado(id);
            }
            return repository.buscarPorId(id)
                    .map(ContratoResponseDTO::new)
                    .orElseThrow(() -> naoEncontrado(id));
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("Não foi possível alterar o status devido a uma inconsistência de integridade.", e);
        }
    }

    @Transactional
    public void excluir(Integer id) {
        validarId(id);
        Contrato atual = repository.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> naoEncontrado(id));

        if (atual.status() == StatusContrato.ENCERRADO || atual.status() == StatusContrato.RESCINDIDO) {
            throw erro(HttpStatus.CONFLICT,
                    "Contratos encerrados ou rescindidos não podem ser excluídos.");
        }

        if (repository.possuiPrestacoes(id)) {
            throw erro(HttpStatus.CONFLICT,
                    "O contrato possui prestações vinculadas e não pode ser excluído. Encerre ou rescinda o contrato para preservar seu histórico.");
        }

        try {
            if (repository.excluir(id) == 0) {
                throw naoEncontrado(id);
            }
        } catch (DataIntegrityViolationException e) {
            throw conflitoIntegridade("O contrato possui registros financeiros vinculados e não pode ser excluído.", e);
        }
    }

    private void validarDadosPrestacao(Contrato contrato, ds.FinHut8.contrato.dtos.PrestacaoCreateDTO dto) {
        if (dto == null || dto.numero() == null || dto.numero() <= 0 || dto.valor() == null
                || dto.valor().signum() <= 0 || dto.valor().compareTo(VALOR_MAXIMO) > 0
                || dto.valor().stripTrailingZeros().scale() > 2 || dto.dataVencimento() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe número positivo, valor positivo com até duas casas decimais e data de vencimento.");
        }
        if (dto.dataVencimento().isBefore(contrato.dataInicio()) || dto.dataVencimento().isAfter(contrato.dataFim())) {
            throw erro(HttpStatus.BAD_REQUEST, "A data de vencimento deve estar dentro do período do contrato.");
        }
    }

    private DadosContrato validarDados(Integer projetoId, String clienteNome, String arquivoRef,
                                       BigDecimal valorTotal, java.time.LocalDate dataInicio,
                                       java.time.LocalDate dataFim) {
        if (projetoId == null || projetoId <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um ID de projeto positivo.");
        }
        if (clienteNome == null || clienteNome.isBlank()) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o nome do cliente.");
        }
        String nomeNormalizado = clienteNome.strip();
        if (nomeNormalizado.codePointCount(0, nomeNormalizado.length()) > CLIENTE_NOME_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "O nome do cliente deve ter no máximo 35 caracteres.");
        }
        if (valorTotal == null || valorTotal.signum() <= 0 || valorTotal.compareTo(VALOR_MAXIMO) > 0
                || valorTotal.stripTrailingZeros().scale() > 2) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "Informe um valor maior que zero, até 9999999,99 e com no máximo duas casas decimais.");
        }
        if (dataInicio == null || dataFim == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe as datas de início e fim do contrato.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw erro(HttpStatus.BAD_REQUEST, "A data de fim não pode ser anterior à data de início.");
        }

        String arquivoNormalizado = arquivoRef == null || arquivoRef.isBlank() ? null : arquivoRef.strip();
        return new DadosContrato(projetoId, nomeNormalizado, arquivoNormalizado,
                valorTotal, dataInicio, dataFim);
    }

    private void validarProjeto(Integer projetoId) {
        if (!repository.projetoExiste(projetoId)) {
            throw erro(HttpStatus.NOT_FOUND, "Projeto não encontrado.");
        }
    }

    private void validarTransicao(StatusContrato atual, StatusContrato destino) {
        if (atual == destino) {
            throw erro(HttpStatus.CONFLICT, "O contrato já possui esse status.");
        }
        boolean permitida = switch (atual) {
            case RASCUNHO -> destino == StatusContrato.ATIVO;
            case ATIVO -> destino == StatusContrato.ENCERRADO || destino == StatusContrato.RESCINDIDO;
            case ENCERRADO, RESCINDIDO -> false;
        };
        if (!permitida) {
            throw erro(HttpStatus.CONFLICT,
                    "Transição de status não permitida: " + atual + " para " + destino + ".");
        }
    }

    private String normalizarFiltro(String filtro) {
        return filtro == null || filtro.isBlank() ? null : filtro.strip();
    }

    private void validarId(Integer id) {
        if (id == null || id <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O ID do contrato deve ser positivo.");
        }
    }

    private ResponseStatusException naoEncontrado(Integer id) {
        return erro(HttpStatus.NOT_FOUND, "Contrato " + id + " não encontrado.");
    }

    private ResponseStatusException conflitoIntegridade(String mensagem, Exception causa) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem, causa);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    private record DadosContrato(Integer projetoId, String clienteNome, String arquivoRef,
                                 BigDecimal valorTotal, java.time.LocalDate dataInicio,
                                 java.time.LocalDate dataFim) {
    }
}
