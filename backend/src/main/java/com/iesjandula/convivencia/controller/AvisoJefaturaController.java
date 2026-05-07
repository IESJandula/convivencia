package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.entity.AvisoJefatura;
import com.iesjandula.convivencia.repository.AvisoJefaturaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.iesjandula.convivencia.entity.Alumno;
import com.iesjandula.convivencia.repository.ParteDisciplinarioRepository;
import com.iesjandula.convivencia.repository.ExpulsionRepository;
import com.iesjandula.convivencia.entity.ParteDisciplinario;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/avisos")
@CrossOrigin(origins = "*")
public class AvisoJefaturaController {

    private final AvisoJefaturaRepository avisoRepository;
    private final ParteDisciplinarioRepository parteRepository;
    private final ExpulsionRepository expulsionRepository;

    public AvisoJefaturaController(AvisoJefaturaRepository avisoRepository,
                                   ParteDisciplinarioRepository parteRepository,
                                   ExpulsionRepository expulsionRepository) {
        this.avisoRepository = avisoRepository;
        this.parteRepository = parteRepository;
        this.expulsionRepository = expulsionRepository;
    }

    @GetMapping
    public ResponseEntity<List<AvisoJefatura>> listarAvisos(@RequestParam(required = false, defaultValue = "false") boolean soloNoLeidos) {
        if (soloNoLeidos) {
            return ResponseEntity.ok(avisoRepository.findByLeidoFalseAndActivoTrueOrderByFechaCreacionDesc());
        }
        return ResponseEntity.ok(avisoRepository.findByActivoTrueOrderByFechaCreacionDesc());
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeido(@PathVariable Integer id) {
        AvisoJefatura aviso = avisoRepository.findById(id).orElseThrow(() -> new RuntimeException("Aviso no encontrado"));
        aviso.setLeido(true);
        avisoRepository.save(aviso);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generar-retroactivos")
    public ResponseEntity<String> generarRetroactivos() {
        List<ParteDisciplinario> activos = parteRepository.findByActivoTrue();
        int creados = 0;
        
        var agrupadosPorAlumno = activos.stream()
                .filter(p -> p.getGravedad() == ParteDisciplinario.Gravedad.LEVE && p.getEstadoComputo() == ParteDisciplinario.EstadoComputo.ACTIVO)
                .collect(Collectors.groupingBy(p -> p.getAlumno()));

        for (var entry : agrupadosPorAlumno.entrySet()) {
            Alumno alumno = entry.getKey();
            long count = entry.getValue().size();
            
            if (count >= 3) {
                long exp = expulsionRepository.findByAlumnoIdAndActivoTrue(alumno.getId()).size();
                if (exp == 0) {
                    boolean tieneAviso = avisoRepository.findByLeidoFalseAndActivoTrueOrderByFechaCreacionDesc().stream()
                            .anyMatch(a -> a.getAlumno().getId().equals(alumno.getId()));
                    if (!tieneAviso) {
                        AvisoJefatura aviso = new AvisoJefatura();
                        aviso.setAlumno(alumno);
                        aviso.setMensaje("El alumno " + alumno.getNombre() + " " + alumno.getApellidos() + " ha acumulado " + count + " partes leves (Alerta retroactiva).");
                        avisoRepository.save(aviso);
                        creados++;
                    }
                }
            }
        }
        return ResponseEntity.ok("Generados " + creados + " avisos retroactivos");
    }
}
