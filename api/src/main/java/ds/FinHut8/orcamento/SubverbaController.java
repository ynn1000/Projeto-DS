package ds.FinHut8.orcamento;

import ds.FinHut8.orcamento.dtos.SubverbaCreateDTO;
import ds.FinHut8.orcamento.dtos.SubverbaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/orcamentos/{periodoId}/verbas/{categoriaId}/subverbas")
@PreAuthorize("hasRole('ADMIN')")
public class SubverbaController {
    private final SubverbaService service;

    public SubverbaController(SubverbaService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubverbaDTO> listar(@PathVariable int periodoId, @PathVariable int categoriaId) {
        return service.listar(periodoId, categoriaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubverbaDTO criar(@PathVariable int periodoId, @PathVariable int categoriaId,
                            @RequestBody SubverbaCreateDTO dto) {
        return service.criar(periodoId, categoriaId, dto);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responderErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> responderJsonInvalido() {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Informe nome e valorReservado em um JSON válido."));
    }
}
