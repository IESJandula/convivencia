package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.ActualizarTareaExpulsionRequestDto;
import com.iesjandula.convivencia.dto.PageResponse;
import com.iesjandula.convivencia.dto.TareaExpulsionDto;
import com.iesjandula.convivencia.entity.TareaExpulsion;
import com.iesjandula.convivencia.service.TareaExpulsionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<PageResponse<TareaExpulsionDto>> listarPorProfesor(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String estado
    ) {
        Page<TareaExpulsionDto> resultado;
        if (estado == null || estado.isBlank()) {
            PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")));
            resultado = tareaExpulsionService.listarPorProfesorPaginado(email, pageable);
        } else {
            TareaExpulsion.Estado estadoEnum = TareaExpulsion.Estado.valueOf(estado.trim().toUpperCase());
            PageRequest pageable = estadoEnum == TareaExpulsion.Estado.COMPLETADA
                    ? PageRequest.of(page, size, Sort.by(Sort.Order.desc("fechaCompletada"), Sort.Order.desc("id")))
                    : PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")));
            resultado = tareaExpulsionService.listarPorProfesorPaginado(email, estadoEnum, pageable);
        }
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @GetMapping("/profesor/{email}/resumen")
    public ResponseEntity<Map<String, Long>> resumenPorProfesor(@PathVariable String email) {
        long pendientes = tareaExpulsionService.contarPorProfesorYEstado(email, com.iesjandula.convivencia.entity.TareaExpulsion.Estado.PENDIENTE);
        long completadas = tareaExpulsionService.contarPorProfesorYEstado(email, com.iesjandula.convivencia.entity.TareaExpulsion.Estado.COMPLETADA);
        long total = tareaExpulsionService.contarPorProfesor(email);
        return ResponseEntity.ok(Map.of(
                "pendientes", pendientes,
                "completadas", completadas,
                "total", total
        ));
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
