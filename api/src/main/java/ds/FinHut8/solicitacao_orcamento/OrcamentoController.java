package ds.FinHut8.solicitacao_orcamento;

import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoDecisaoDTO;
import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoRequestDTO;
import ds.FinHut8.solicitacao_orcamento.dtos.OrcamentoResponseDTO;
import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;
import ds.FinHut8.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitar_orcamento")
public class OrcamentoController {

    private final OrcamentoService service;

    public OrcamentoController(OrcamentoService service) {
        this.service = service;
    }

    // Usuário: solicitar orçamento
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO solicitar(
            @RequestBody OrcamentoRequestDTO dto,
            @AuthenticationPrincipal User usuario) {

        return service.solicitar(dto, usuario);
    }

    // Usuário: consultar as próprias solicitações
    @GetMapping("/minhas")
    public List<OrcamentoResponseDTO> listarMinhas(@AuthenticationPrincipal User usuario) {
        return service.listarMinhas(usuario);
    }

    // Admin: consultar todas (filtro opcional: /orcamentos?status=PENDENTE)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<OrcamentoResponseDTO> listarTodas(
            @RequestParam(required = false) OrcamentoStatus status) {

        return service.listarTodas(status);
    }

    // Dono ou admin: consultar uma solicitação
    @GetMapping("/{id}")
    public OrcamentoResponseDTO consultar(
            @PathVariable Integer id,
            @AuthenticationPrincipal User usuario) {

        return service.consultar(id, usuario);
    }

    // Admin: alterar o status
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public OrcamentoResponseDTO alterarStatus(
            @PathVariable Integer id,
            @RequestBody OrcamentoDecisaoDTO dto,
            @AuthenticationPrincipal User admin) {

        return service.alterarStatus(id, dto, admin);
    }
}
