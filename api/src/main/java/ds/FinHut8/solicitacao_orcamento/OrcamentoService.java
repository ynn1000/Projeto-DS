package ds.FinHut8.solicitacao_orcamento;

import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoDecisaoDTO;
import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoRequestDTO;
import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoResponseDTO;
import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;
import ds.FinHut8.user.User;
import ds.FinHut8.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrcamentoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999.99");
    private static final int TITULO_MAX = 150;

    private final OrcamentoRepository repository;

    public OrcamentoService(OrcamentoRepository repository) {
        this.repository = repository;
    }

    // Cria uma solicitação, PENDENTE como valor padrão
    public OrcamentoResponseDTO solicitar(OrcamentoRequestDTO dto, User solicitante) {
        validarSolicitacao(dto);

        if (!repository.categoriaAtivaExiste(dto.categoriaId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Categoria inexistente ou inativa."
            );
        }

        Integer id = repository.inserir(
                funcionarioDe(solicitante),
                dto.categoriaId(),
                dto.titulo().trim(),
                dto.descricao().trim(),
                dto.valorSolicitado(),
                dto.arquivoRef()
        );

        return new OrcamentoResponseDTO(buscarOuFalhar(id));
    }

    // Lista as solicitações do próprio usuário
    public List<OrcamentoResponseDTO> listarMinhas(User usuario) {
        return repository.findBySolicitante(funcionarioDe(usuario))
                .stream()
                .map(OrcamentoResponseDTO::new)
                .toList();
    }

    // USER ou ADMIN consultam uma solicitação, o USER só pode consultar a sua própria
    public OrcamentoResponseDTO consultar(Integer id, User usuario) {
        Orcamento orcamento = buscarOuFalhar(id);

        boolean admin = usuario.getRole() == UserRole.ADMIN;
        boolean dono = repository.findFuncionarioIdByUsuario(usuario.getId())
                .map(orcamento.solicitanteId()::equals)
                .orElse(false);

        if (!admin && !dono) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem acesso a esta solicitação"
            );
        }

        return new OrcamentoResponseDTO(orcamento);
    }

    // Lista todas as solicitações
    public List<OrcamentoResponseDTO> listarTodas(OrcamentoStatus status) {
        return repository.findAll(status)
                .stream()
                .map(OrcamentoResponseDTO::new)
                .toList();
    }

    // Altera o status: PENDENTE -> EM_ANALISE | APROVADO | REJEITADO 
    public OrcamentoResponseDTO alterarStatus(Integer id, OrcamentoDecisaoDTO dto, User admin) {
        if (dto == null || dto.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o novo status:");
        }

        OrcamentoStatus novo = dto.status();
        String justificativa = dto.justificativa() == null ? null : dto.justificativa().trim();
        if (justificativa != null && justificativa.isEmpty()) {
            justificativa = null;
        }

        if (novo == OrcamentoStatus.REPROVADO && justificativa == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe a justificativa para rejeitar a solicitação:"
            );
        }

        Orcamento atual = buscarOuFalhar(id);

        if (!transicaoPermitida(atual.status(), novo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível mudar o status de " + atual.status() + " para " + novo + "."
            );
        }

        // Registra quem avaliou e quando
        boolean decisaoFinal = novo == OrcamentoStatus.APROVADO || novo == OrcamentoStatus.REPROVADO;

        Integer avaliadorId = decisaoFinal ? funcionarioDe(admin) : null;

        int alteradas = repository.atualizarStatus(
                id,
                atual.status(),
                novo,
                avaliadorId,
                justificativa,
                decisaoFinal ? OffsetDateTime.now() : null
        );

        if (alteradas == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O status desta solicitação foi alterado por outra pessoa. Consulte novamente."
            );
        }

        return new OrcamentoResponseDTO(buscarOuFalhar(id));
    }



    private boolean transicaoPermitida(OrcamentoStatus atual, OrcamentoStatus novo) {
        return switch (atual) {
            case PENDENTE -> novo == OrcamentoStatus.EM_ANALISE
                    || novo == OrcamentoStatus.APROVADO
                    || novo == OrcamentoStatus.REPROVADO;
            case EM_ANALISE -> novo == OrcamentoStatus.APROVADO
                    || novo == OrcamentoStatus.REPROVADO;
            default -> false; // APROVADO e REJEITADO são finais
        };
    }

    private void validarSolicitacao(OrcamentoRequestDTO dto) {
        if (dto == null
                || dto.categoriaId() == null
                || dto.titulo() == null || dto.titulo().isBlank()
                || dto.descricao() == null || dto.descricao().isBlank()
                || dto.valorSolicitado() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe categoria, título, descrição e valor solicitado:"
            );
        }

        if (dto.titulo().trim().length() > TITULO_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O título deve ter no máximo " + TITULO_MAX + " caracteres."
            );
        }

        BigDecimal valor = dto.valorSolicitado();

        if (valor.signum() <= 0
                || valor.compareTo(VALOR_MAXIMO) > 0
                || valor.stripTrailingZeros().scale() > 2) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor deve ser maior que zero, até 9.999.999,99, com no máximo duas casas decimais."
            );
        }
    }

    // Funcionário ligado ao usuário logado, sem vínculo não dá para solicitar/avaliar
    private Integer funcionarioDe(User usuario) {
        return repository.findFuncionarioIdByUsuario(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuário não vinculado a um funcionário."
                ));
    }

    private Orcamento buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitação de orçamento não encontrada."
                ));
    }
}
