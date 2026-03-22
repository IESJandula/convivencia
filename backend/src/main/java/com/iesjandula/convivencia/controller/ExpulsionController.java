package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.CrearExpulsionRequestDto;
import com.iesjandula.convivencia.dto.CrearExpulsionResponseDto;
import com.iesjandula.convivencia.dto.ExpulsionPdfItemDto;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.service.ExpulsionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expulsiones")
@CrossOrigin(origins = "*")
public class ExpulsionController {

    private final ExpulsionService expulsionService;

    public ExpulsionController(ExpulsionService expulsionService) {
        this.expulsionService = expulsionService;
    }

    @GetMapping("/alumno/{alumnoId}/partes-activos")
    public ResponseEntity<List<ParteDisciplinario>> obtenerPartesActivosAlumno(@PathVariable Integer alumnoId) {
        return ResponseEntity.ok(expulsionService.obtenerPartesActivosAlumno(alumnoId));
    }

    @PostMapping
    public ResponseEntity<?> crearExpulsion(@RequestBody CrearExpulsionRequestDto request) {
        try {
            CrearExpulsionResponseDto response = expulsionService.crearExpulsion(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/{expulsionId}/pdf")
    public ResponseEntity<?> descargarCartaExpulsion(@PathVariable Integer expulsionId) {
        try {
            byte[] pdf = expulsionService.generarCartaExpulsionPdf(expulsionId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=carta-expulsion-" + expulsionId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{expulsionId}/puede-generar-pdf")
    public ResponseEntity<Map<String, Boolean>> puedeGenerarPdf(@PathVariable Integer expulsionId) {
        boolean puedeGenerar = expulsionService.puedeGenerarCartaExpulsion(expulsionId);
        return ResponseEntity.ok(Map.of("puedeGenerar", puedeGenerar));
    }

    @GetMapping("/pendientes-pdf")
    public ResponseEntity<List<ExpulsionPdfItemDto>> listarPendientesPdf() {
        return ResponseEntity.ok(expulsionService.listarExpulsionesParaPdf());
    }
}
