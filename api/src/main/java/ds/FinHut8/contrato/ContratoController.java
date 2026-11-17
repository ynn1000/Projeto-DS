package ds.FinHut8.contrato;

import ds.FinHut8.contrato.dtos.ContratoCreateDTO;
import ds.FinHut8.contrato.dtos.ContratoResponseDTO;
import ds.FinHut8.contrato.dtos.ContratoStatusUpdateDTO;
import ds.FinHut8.contrato.dtos.ContratoUpdateDTO;
import ds.FinHut8.contrato.enums.StatusContrato;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/contratos")
@PreAuthorize("hasRole('ADMIN')")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContratoResponseDTO> listar(
            @RequestParam(required = false) Integer projetoId,
            @RequestParam(required = false) String clienteNome,
            @RequestParam(required = false) StatusContrato status) {
        return service.listar(projetoId, clienteNome, status);
    }

    @GetMapping("/{id}")
    public ContratoResponseDTO buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ContratoResponseDTO> criar(@RequestBody ContratoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ContratoResponseDTO atualizar(@PathVariable Integer id,
                                         @RequestBody ContratoUpdateDTO dto) {
        return service.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    public ContratoResponseDTO atualizarStatus(@PathVariable Integer id,
                                               @RequestBody ContratoStatusUpdateDTO dto) {
        return service.atualizarStatus(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        service.excluir(id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responderErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> responderJsonInvalido(HttpMessageNotReadableException erro) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou contém JSON inválido. Confira os campos e seus tipos."
        );
        return ResponseEntity.badRequest().body(detalhe);
    }
}
