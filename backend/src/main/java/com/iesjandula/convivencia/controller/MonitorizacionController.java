package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.entity.Grupo;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.service.MonitorizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/monitorizacion")
@CrossOrigin(origins = "*")
public class MonitorizacionController {

    private final MonitorizacionService monitorizacionService;

    public MonitorizacionController(MonitorizacionService monitorizacionService) {
        this.monitorizacionService = monitorizacionService;
    }

    @GetMapping("/tutor/{email}")
    public ResponseEntity<Map<String, Object>> obtenerMonitorizacionTutor(@PathVariable String email) {
        Optional<Grupo> grupoTutoria = monitorizacionService.obtenerGrupoTutoria(email);
        List<ParteDisciplinario> partes = monitorizacionService.obtenerPartesTutoria(email);

        Map<String, Object> response = new HashMap<>();
        response.put("grupoTutoria", grupoTutoria.orElse(null));
        response.put("partes", partes);
        response.put("totalPartes", partes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jefatura")
    public ResponseEntity<List<ParteDisciplinario>> obtenerMonitorizacionJefatura(
            @RequestParam(required = false) String nombreAlumno,
            @RequestParam(required = false) String curso,
            @RequestParam(required = false) String grupo
    ) {
        return ResponseEntity.ok(monitorizacionService.obtenerPartesJefatura(nombreAlumno, curso, grupo));
    }

    @GetMapping("/jefatura/filtros")
    public ResponseEntity<Map<String, Object>> obtenerFiltrosJefatura() {
        Map<String, Object> response = new HashMap<>();
        response.put("cursos", monitorizacionService.obtenerCursosJefatura());
        response.put("grupos", monitorizacionService.obtenerGruposJefatura());
        return ResponseEntity.ok(response);
    }
}
