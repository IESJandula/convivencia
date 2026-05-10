package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.AlumnoDto;
import com.iesjandula.convivencia.entity.Alumno;
import com.iesjandula.convivencia.repository.AlumnoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alumnos")
@CrossOrigin(origins = "*")
public class AlumnoController {

    private final AlumnoRepository alumnoRepository;
    private final com.iesjandula.convivencia.repository.GrupoRepository grupoRepository;

    public AlumnoController(AlumnoRepository alumnoRepository, com.iesjandula.convivencia.repository.GrupoRepository grupoRepository) {
        this.alumnoRepository = alumnoRepository;
        this.grupoRepository = grupoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Alumno>> listarTodos() {
        return ResponseEntity.ok(alumnoRepository.findDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alumno> obtenerPorId(@PathVariable Integer id) {
        return alumnoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/curso/{curso}")
    public ResponseEntity<List<Alumno>> listarPorCurso(@PathVariable String curso) {
        return ResponseEntity.ok(alumnoRepository.findByCursoDisponibles(curso));
    }

    @GetMapping("/debug/raw")
    public ResponseEntity<Map<String, Object>> debugRaw() {
        List<Alumno> raw = alumnoRepository.findAll();
        List<Alumno> disponibles = alumnoRepository.findDisponibles();
        return ResponseEntity.ok(Map.of(
                "totalRaw", raw.size(),
                "totalDisponibles", disponibles.size(),
                "raw", raw,
                "disponibles", disponibles
        ));
    }

    @PostMapping
    public ResponseEntity<Alumno> crearAlumno(@RequestBody AlumnoDto dto) {
        Alumno alumno = new Alumno();
        alumno.setNombre(dto.getNombre());
        alumno.setApellidos(dto.getApellidos());
        
        com.iesjandula.convivencia.entity.Grupo grupo = grupoRepository.findByCursoAndLetraAndActivoTrue(dto.getCurso(), dto.getGrupo())
            .orElseGet(() -> {
                com.iesjandula.convivencia.entity.Grupo nuevo = new com.iesjandula.convivencia.entity.Grupo();
                nuevo.setCurso(dto.getCurso());
                nuevo.setLetra(dto.getGrupo());
                nuevo.setActivo(true);
                return grupoRepository.save(nuevo);
            });

        alumno.setGrupo(grupo);
        alumno.setActivo(true);
        return ResponseEntity.ok(alumnoRepository.save(alumno));
    }
}
