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

    public AlumnoController(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
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
        alumno.setCurso(dto.getCurso());
        alumno.setGrupo(dto.getGrupo());
        alumno.setActivo(true);
        return ResponseEntity.ok(alumnoRepository.save(alumno));
    }
}
