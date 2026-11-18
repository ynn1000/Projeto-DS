package ds.FinHut8.dashboard;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private final DashboardRepository repository;
    private final CaixaRepository caixaRepository;

    public DashboardController(DashboardRepository repository, CaixaRepository caixaRepository) {
        this.repository = repository;
        this.caixaRepository = caixaRepository;
    }

    @GetMapping("/recebimentos")
    public DashboardRepository.Recebimentos recebimentos() {
        return repository.recebimentos(caixaId());
    }

    @GetMapping("/opcoes-lancamento")
    public DashboardRepository.OpcoesLancamento opcoesLancamento(
            @RequestParam(name = "dataMovimento") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataMovimento) {
        return repository.opcoesLancamento(caixaId(), dataMovimento);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responderErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    private int caixaId() {
        return caixaRepository.find().map(Caixa::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caixa não encontrado."));
    }
}
