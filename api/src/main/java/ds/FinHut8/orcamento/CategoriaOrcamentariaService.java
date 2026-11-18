package ds.FinHut8.orcamento;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class CategoriaOrcamentariaService {
    private final CategoriaOrcamentariaRepository repository;
    private final CaixaRepository caixaRepository;
    public CategoriaOrcamentariaService(CategoriaOrcamentariaRepository repository, CaixaRepository caixaRepository) {
        this.repository = repository; this.caixaRepository = caixaRepository;
    }
    public List<CategoriaOrcamentariaDTO> listarAtivas() {
        Integer caixaId = caixaRepository.find().map(Caixa::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caixa não encontrado."));
        return repository.listarAtivasDoCaixa(caixaId);
    }
}
