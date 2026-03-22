package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.AsignarTutorGrupoDto;
import com.iesjandula.convivencia.entity.Grupo;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.GrupoRepository;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/grupos")
@CrossOrigin(origins = "*")
public class GrupoController {

    private final GrupoRepository grupoRepository;
    private final ProfesorRepository profesorRepository;

    public GrupoController(GrupoRepository grupoRepository, ProfesorRepository profesorRepository) {
        this.grupoRepository = grupoRepository;
        this.profesorRepository = profesorRepository;
    }

    @PostMapping("/asignar-tutor")
    public ResponseEntity<?> asignarTutor(@RequestBody AsignarTutorGrupoDto dto) {
        if (dto.getCurso() == null || dto.getCurso().trim().isEmpty() ||
                dto.getLetra() == null || dto.getLetra().trim().isEmpty() ||
                dto.getTutorEmail() == null || dto.getTutorEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "curso, letra y tutorEmail son obligatorios"));
        }

        Profesor tutor = profesorRepository.findByEmailNormalized(dto.getTutorEmail().trim())
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        grupoRepository.findByTutorEmailAndActivoTrue(tutor.getEmail())
            .ifPresent(grupoAnterior -> {
                if (!(grupoAnterior.getCurso().equalsIgnoreCase(dto.getCurso().trim())
                    && grupoAnterior.getLetra().equalsIgnoreCase(dto.getLetra().trim()))) {
                grupoAnterior.setTutor(null);
                grupoRepository.save(grupoAnterior);
                }
            });

        Grupo grupo = grupoRepository.findByCursoAndActivoTrue(dto.getCurso().trim()).stream()
                .filter(g -> dto.getLetra().trim().equalsIgnoreCase(g.getLetra()))
                .findFirst()
                .orElseGet(() -> {
                    Grupo nuevo = new Grupo();
                    nuevo.setCurso(dto.getCurso().trim());
                    nuevo.setLetra(dto.getLetra().trim().toUpperCase());
                    nuevo.setActivo(true);
                    return nuevo;
                });

        grupo.setTutor(tutor);
        grupo.setActivo(true);

        Grupo guardado = grupoRepository.save(grupo);

        Map<String, Object> response = new HashMap<>();
        response.put("id", guardado.getId());
        response.put("curso", guardado.getCurso());
        response.put("letra", guardado.getLetra());
        response.put("tutorEmail", guardado.getTutor() != null ? guardado.getTutor().getEmail() : null);
        response.put("message", "Tutor asignado correctamente");

        return ResponseEntity.ok(response);
    }
}
