package ds.FinHut8.orcamento;

import ds.FinHut8.caixa.Caixa;
import ds.FinHut8.caixa.CaixaRepository;
import ds.FinHut8.orcamento.dtos.SubverbaCreateDTO;
import ds.FinHut8.orcamento.dtos.SubverbaDTO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@Service
public class SubverbaService {
    private static final BigDecimal MAXIMO = new BigDecimal("9999999.99");

    private final SubverbaRepository repository;
    private final CaixaRepository caixaRepository;

    public SubverbaService(SubverbaRepository repository, CaixaRepository caixaRepository) {
        this.repository = repository;
        this.caixaRepository = caixaRepository;
    }

    public List<SubverbaDTO> listar(int periodoId, int categoriaId) {
        if (!repository.distribuicaoExiste(caixaId(), periodoId, categoriaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Verba não encontrada neste período.");
        }
        return repository.listar(periodoId, categoriaId);
    }

    public SubverbaDTO criar(int periodoId, int categoriaId, SubverbaCreateDTO dto) {
        if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome da subverba.");
        }
        String nome = dto.nome().strip();
        if (nome.codePointCount(0, nome.length()) > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome deve ter até 80 caracteres.");
        }
        BigDecimal valor = dto.valorReservado();
        if (valor == null || valor.signum() <= 0 || valor.compareTo(MAXIMO) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um valor reservado positivo, até R$ 9.999.999,99 e com até duas casas decimais.");
        }

        try {
            return repository.criar(caixaId(), periodoId, categoriaId, nome, valor)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Verba ativa não encontrada em um período aberto."));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma subverba com esse nome.", e);
        } catch (DataAccessException e) {
            if (e.getMostSpecificCause() instanceof SQLException causa
                    && "P5001".equals(causa.getSQLState())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A soma das reservas ultrapassa a verba principal.", e);
            }
            throw e;
        }
    }

    private int caixaId() {
        return caixaRepository.find().map(Caixa::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caixa não encontrado."));
    }
}
