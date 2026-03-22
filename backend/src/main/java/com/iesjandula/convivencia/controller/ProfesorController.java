package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.ProfesorDto;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profesores")
@CrossOrigin(origins = "*")
public class ProfesorController {

    private final ProfesorRepository profesorRepository;

    public ProfesorController(ProfesorRepository profesorRepository) {
        this.profesorRepository = profesorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Profesor>> listarTodos() {
        return ResponseEntity.ok(profesorRepository.findDisponibles());
    }

    @GetMapping("/{email}")
    public ResponseEntity<Profesor> obtenerPorEmail(@PathVariable String email) {
        return profesorRepository.findById(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/guardias")
    public ResponseEntity<List<Profesor>> listarProfesoresGuardia() {
        return ResponseEntity.ok(profesorRepository.findByEsGuardiaAndActivoTrue(true));
    }

    @GetMapping("/debug/raw")
    public ResponseEntity<Map<String, Object>> debugRaw() {
        List<Profesor> todos = profesorRepository.findAll();
        List<Profesor> disponibles = profesorRepository.findDisponibles();
        return ResponseEntity.ok(Map.of(
                "totalRaw", todos.size(),
                "totalDisponibles", disponibles.size(),
                "raw", todos,
                "disponibles", disponibles
        ));
    }

    @PostMapping
    public ResponseEntity<Profesor> crearProfesor(@RequestBody ProfesorDto dto) {
        Profesor profesor = new Profesor();
        profesor.setEmail(dto.getEmail());
        profesor.setNombre(dto.getNombre());
        profesor.setRol(dto.getRol() != null ? Profesor.Rol.valueOf(dto.getRol()) : Profesor.Rol.PROFESOR);
        profesor.setEsGuardia(dto.getEsGuardia() != null ? dto.getEsGuardia() : false);
        profesor.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return ResponseEntity.ok(profesorRepository.save(profesor));
    }
}
