package ds.FinHut8.transacao;

import ds.FinHut8.transacao.dtos.TransacaoCreateDTO;
import ds.FinHut8.transacao.dtos.TransacaoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
@PreAuthorize("hasRole('ADMIN')")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TransacaoResponseDTO> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponseDTO registrar(@RequestBody TransacaoCreateDTO dto) {
        return service.registrar(dto);
    }

    // Responde aqui para evitar que o redirecionamento interno para /error
    // transforme uma falha de validação em um 403 do Spring Security.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responderErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> responderJsonInvalido() {
        ProblemDetail erro = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Confira o tipo (RECEITA ou DESPESA), a categoria da transação e a data (AAAA-MM-DD).");
        return ResponseEntity.badRequest().body(erro);
    }
}
