package ds.FinHut8.caixa;

import ds.FinHut8.caixa.dtos.CaixaResponseDTO;
import ds.FinHut8.caixa.dtos.CaixaUpdateDTO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class CaixaService {

    private static final BigDecimal VALOR_MAXIMO =
            new BigDecimal("9999999.99");

    private final CaixaRepository repository;

    public CaixaService(CaixaRepository repository) {
        this.repository = repository;
    }

    // Consulta o caixa 
    public CaixaResponseDTO consultar() {
        Caixa caixa = buscarCaixa();

        return new CaixaResponseDTO(caixa);
    }

    // Altera o limite de despesas
    public CaixaResponseDTO alterarLimite(CaixaUpdateDTO dto) {

        if (dto == null || dto.limiteDespesas() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe o limite de despesas."
            );
        }

        BigDecimal limite = dto.limiteDespesas();

        if (limite.signum() < 0
                || limite.compareTo(VALOR_MAXIMO) > 0
                || limite.stripTrailingZeros().scale() > 2) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O limite deve ser positivo ou zero e ter no máximo duas casas decimais."
            );
        }

        Caixa caixa = buscarCaixa();

        repository.atualizarLimite(caixa.id(), limite);

        return consultar();
    }

    // Método auxiliar para localizar o caixa
    private Caixa buscarCaixa() {

        return repository.find()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Caixa não encontrado."
                ));
    }
}