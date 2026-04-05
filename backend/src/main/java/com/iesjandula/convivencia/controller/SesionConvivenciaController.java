package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.SesionConvivenciaRequestDto;
import com.iesjandula.convivencia.entity.SesionConvivencia;
import com.iesjandula.convivencia.service.SesionConvivenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sesiones")
@CrossOrigin(origins = "*")
public class SesionConvivenciaController {

    private final SesionConvivenciaService sesionService;

    public SesionConvivenciaController(SesionConvivenciaService sesionService) {
        this.sesionService = sesionService;
    }

    @PostMapping
    public ResponseEntity<?> registrarSesion(@RequestBody SesionConvivenciaRequestDto dto) {
        try {
            return ResponseEntity.ok(sesionService.registrarSesion(dto));
        } catch (ResponseStatusException ex) {
            HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
            return ResponseEntity.status(status).body(Map.of("error", ex.getReason() != null ? ex.getReason() : "Error al registrar sesión"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al registrar sesión"));
        }
    }

    @GetMapping
    public ResponseEntity<List<SesionConvivencia>> listarTodas() {
        return ResponseEntity.ok(sesionService.listarTodas());
    }

    @GetMapping("/fecha/{fecha}/tramo/{tramo}")
    public ResponseEntity<List<SesionConvivencia>> listarPorFechaYTramo(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PathVariable String tramo
    ) {
        return ResponseEntity.ok(sesionService.listarPorFechaYTramo(fecha, tramo));
    }

    @GetMapping("/parte/{parteId}")
    public ResponseEntity<List<SesionConvivencia>> obtenerPorParte(@PathVariable Integer parteId) {
        return ResponseEntity.ok(sesionService.obtenerPorParteId(parteId));
    }
}
