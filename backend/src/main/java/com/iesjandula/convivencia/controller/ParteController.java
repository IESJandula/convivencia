package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.ParteAulaConvivenciaDto;
import com.iesjandula.convivencia.dto.ParteRequestDto;
import com.iesjandula.convivencia.dto.PageResponse;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.service.ParteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<PageResponse<ParteDisciplinario>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String alumnoTexto,
            @RequestParam(required = false) String curso,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String profesorTexto,
            @RequestParam(required = false) String gravedad,
            @RequestParam(required = false) String conductaTexto,
            @RequestParam(required = false) String estadoComputo,
            @RequestParam(required = false) Integer alumnoId
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        Page<ParteDisciplinario> resultado = parteService.listarHistorialPaginado(
                null,
                fechaDesde,
                fechaHasta,
                alumnoTexto,
                curso,
                grupo,
                profesorTexto,
                gravedad,
                conductaTexto,
                estadoComputo,
                alumnoId,
                pageable
        );
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParteDisciplinario> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(parteService.obtenerPorId(id));
    }

    @GetMapping("/profesor/{email}")
    public ResponseEntity<PageResponse<ParteDisciplinario>> listarPorProfesor(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String alumnoTexto,
            @RequestParam(required = false) String curso,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String profesorTexto,
            @RequestParam(required = false) String gravedad,
            @RequestParam(required = false) String conductaTexto,
            @RequestParam(required = false) String estadoComputo,
            @RequestParam(required = false) Integer alumnoId
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        Page<ParteDisciplinario> resultado = parteService.listarHistorialPaginado(
                email,
                fechaDesde,
                fechaHasta,
                alumnoTexto,
                curso,
                grupo,
                profesorTexto,
                gravedad,
                conductaTexto,
                estadoComputo,
                alumnoId,
                pageable
        );
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<ParteDisciplinario>> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(parteService.listarPorFecha(fecha));
    }

    @GetMapping("/aula-convivencia")
    public ResponseEntity<PageResponse<ParteAulaConvivenciaDto>> listarPartesAulaConvivencia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String curso,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalDate fechaBusqueda = fecha != null ? fecha : LocalDate.now();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        Page<ParteAulaConvivenciaDto> resultado = parteService.listarPartesAulaConvivenciaPaginado(fechaBusqueda, curso, grupo, pageable);
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarParte(@PathVariable Integer id) {
        parteService.eliminarParte(id);
        return ResponseEntity.noContent().build();
    }
}
