package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.entity.ConductaConvivencia;
import com.iesjandula.convivencia.repository.ConductaConvivenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conductas")
@CrossOrigin(origins = "*")
public class ConductaController {

    private static final Set<String> CODIGOS_RD327 = Set.of(
            "1a", "1b", "1c", "1d", "1e", "1f", "1g",
            "2a", "2b", "2c", "2d", "2e", "2f", "2g", "2h", "2i", "2j", "2k"
    );

    private final ConductaConvivenciaRepository conductaRepository;

    public ConductaController(ConductaConvivenciaRepository conductaRepository) {
        this.conductaRepository = conductaRepository;
    }

    @GetMapping
    public ResponseEntity<List<ConductaConvivencia>> listarTodas(
            @RequestParam(required = false) String gravedad,
            @RequestParam(defaultValue = "true") boolean incluir2i
    ) {
        List<ConductaConvivencia> conductas;

        if (gravedad != null && !gravedad.isBlank()) {
            ConductaConvivencia.Gravedad gravedadEnum;
            try {
                gravedadEnum = ConductaConvivencia.Gravedad.valueOf(
                        gravedad.trim().toUpperCase()
                );
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().build();
            }
            conductas = conductaRepository.findByGravedadOrderByCodigoAsc(gravedadEnum);
        } else {
            conductas = conductaRepository.findAllByOrderByCodigoAsc();
        }

        if (!incluir2i) {
            conductas = conductas.stream()
                    .filter(c -> c.getCodigo() == null || !"2i".equalsIgnoreCase(c.getCodigo().trim()))
                    .collect(Collectors.toList());
        }

        conductas = conductas.stream()
                .filter(c -> isCodigoRD327(c.getCodigo()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(conductas);
    }

    private boolean isCodigoRD327(String codigo) {
        if (codigo == null) {
            return false;
        }
        return CODIGOS_RD327.contains(codigo.trim().toLowerCase());
    }
}
