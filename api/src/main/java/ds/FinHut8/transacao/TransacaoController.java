package ds.FinHut8.transacao;

import ds.FinHut8.transacao.dtos.ComprovanteCreateDTO;
import ds.FinHut8.transacao.dtos.ComprovanteResponseDTO;
import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import ds.FinHut8.transacao.dtos.TransacaoResponseDTO;
import ds.FinHut8.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transacoes")
@PreAuthorize("hasRole('ADMIN')")
public class TransacaoController {
    private final TransacaoService service;
    public TransacaoController(TransacaoService service) { this.service = service; }

    @GetMapping
    public List<TransacaoResponseDTO> listar(@RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) CategoriaTransacao categoriaTransacao,
            @RequestParam(required = false) Integer categoriaOrcamentariaId,
            @RequestParam(required = false) Integer projetoId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        return service.listar(tipo, categoriaTransacao, categoriaOrcamentariaId, projetoId, dataInicio, dataFim);
    }

    @GetMapping("/opcoes/projetos")
    public List<ProjetoOpcaoDTO> projetosAtivos() { return service.listarProjetosAtivos(); }

    @GetMapping("/opcoes/prestacoes-pendentes")
    public List<PrestacaoOpcaoDTO> prestacoesPendentes() { return service.listarPrestacoesPendentes(); }

    @GetMapping("/opcoes/periodos-orcamento-ativos")
    public List<PeriodoOrcamentoOpcaoDTO> periodosOrcamentoAtivos() { return service.listarPeriodosOrcamentoAtivos(); }

    @GetMapping("/resumo")
    public TransacaoResumo resumo(@RequestParam LocalDate dataInicio, @RequestParam LocalDate dataFim) {
        return service.resumir(dataInicio, dataFim);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponseDTO registrar(@RequestBody TransacaoCreateDTO dto, @AuthenticationPrincipal User usuario) {
        return service.registrar(dto, usuario);
    }

    @GetMapping("/{id}/comprovantes")
    public List<ComprovanteResponseDTO> listarComprovantes(@PathVariable Integer id) {
        return service.listarComprovantes(id);
    }

    @PostMapping("/{id}/comprovantes")
    @ResponseStatus(HttpStatus.CREATED)
    public ComprovanteResponseDTO adicionarComprovante(@PathVariable Integer id, @RequestBody ComprovanteCreateDTO dto) {
        return service.adicionarComprovante(id, dto);
    }

    @GetMapping("/prestacoes/{prestacaoId}/comprovantes")
    public List<ComprovanteResponseDTO> listarComprovantesPrestacao(@PathVariable Integer prestacaoId) {
        return service.listarComprovantesPrestacao(prestacaoId);
    }

    @PostMapping("/prestacoes/{prestacaoId}/comprovantes")
    @ResponseStatus(HttpStatus.CREATED)
    public ComprovanteResponseDTO adicionarComprovantePrestacao(@PathVariable Integer prestacaoId,
            @RequestBody ComprovanteCreateDTO dto) {
        return service.adicionarComprovantePrestacao(prestacaoId, dto);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responderErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> responderJsonInvalido() {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Confira os campos, enums e datas no formato AAAA-MM-DD do JSON enviado."));
    }
}
