package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.ActualizarTareaExpulsionRequestDto;
import com.iesjandula.convivencia.dto.TareaExpulsionDto;
import com.iesjandula.convivencia.service.TareaExpulsionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tareas-expulsion")
@CrossOrigin(origins = "*")
public class TareaExpulsionController {

    private final TareaExpulsionService tareaExpulsionService;

    public TareaExpulsionController(TareaExpulsionService tareaExpulsionService) {
        this.tareaExpulsionService = tareaExpulsionService;
    }

    @GetMapping("/profesor/{email}")
    public ResponseEntity<List<TareaExpulsionDto>> listarPorProfesor(@PathVariable String email) {
        return ResponseEntity.ok(tareaExpulsionService.listarPorProfesor(email));
    }

    @GetMapping("/expulsion/{expulsionId}")
    public ResponseEntity<?> listarPorExpulsion(@PathVariable Integer expulsionId) {
        try {
            return ResponseEntity.ok(tareaExpulsionService.listarPorExpulsion(expulsionId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{tareaId}/completar")
    public ResponseEntity<?> marcarCompletada(@PathVariable Integer tareaId,
                                              @RequestParam String profesorEmail) {
        try {
            return ResponseEntity.ok(tareaExpulsionService.marcarCompletada(tareaId, profesorEmail));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{tareaId}")
    public ResponseEntity<?> actualizarTarea(@PathVariable Integer tareaId,
                                             @RequestBody ActualizarTareaExpulsionRequestDto request) {
        try {
            return ResponseEntity.ok(tareaExpulsionService.actualizarTareaProfesor(
                    tareaId,
                    request.getProfesorEmail(),
                    request.getDescripcionTarea(),
                    request.getCompletar()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
