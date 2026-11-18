package ds.FinHut8.orcamento;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/categorias-orcamentarias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaOrcamentariaController {
    private final CategoriaOrcamentariaService service;
    public CategoriaOrcamentariaController(CategoriaOrcamentariaService service) { this.service = service; }
    @GetMapping
    public List<CategoriaOrcamentariaDTO> listarAtivas() { return service.listarAtivas(); }
}
