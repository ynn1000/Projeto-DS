package ds.FinHut8.caixa;

import ds.FinHut8.caixa.dtos.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caixa")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    @GetMapping
    public CaixaResponseDTO consultar() {
        return service.consultar();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping
    public CaixaResponseDTO alterarLimite(
            @RequestBody CaixaUpdateDTO dto) {

        return service.alterarLimite(dto);
    }
}