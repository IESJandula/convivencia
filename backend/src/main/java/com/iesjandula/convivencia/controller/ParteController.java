package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.ParteAulaConvivenciaDto;
import com.iesjandula.convivencia.dto.ParteRequestDto;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.service.ParteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partes")
@CrossOrigin(origins = "*")
public class ParteController {

    private final ParteService parteService;

    public ParteController(ParteService parteService) {
        this.parteService = parteService;
    }

    @PostMapping
    public ResponseEntity<ParteDisciplinario> crearParte(@RequestBody ParteRequestDto dto) {
        return ResponseEntity.ok(parteService.crearParte(dto));
    }

    @GetMapping
    public ResponseEntity<List<ParteDisciplinario>> listarTodos() {
        return ResponseEntity.ok(parteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParteDisciplinario> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(parteService.obtenerPorId(id));
    }

    @GetMapping("/profesor/{email}")
    public ResponseEntity<List<ParteDisciplinario>> listarPorProfesor(@PathVariable String email) {
        return ResponseEntity.ok(parteService.listarPorProfesor(email));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<ParteDisciplinario>> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(parteService.listarPorFecha(fecha));
    }

    @GetMapping("/aula-convivencia")
    public ResponseEntity<List<ParteAulaConvivenciaDto>> listarPartesAulaConvivencia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalDate fechaBusqueda = fecha != null ? fecha : LocalDate.now();
        return ResponseEntity.ok(parteService.listarPartesAulaConvivencia(fechaBusqueda));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarParte(@PathVariable Integer id) {
        parteService.eliminarParte(id);
        return ResponseEntity.noContent().build();
    }
}
