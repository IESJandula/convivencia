package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.entity.ConductaConvivencia;
import com.iesjandula.convivencia.repository.ConductaConvivenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conductas")
@CrossOrigin(origins = "*")
public class ConductaController {

    private final ConductaConvivenciaRepository conductaRepository;

    public ConductaController(ConductaConvivenciaRepository conductaRepository) {
        this.conductaRepository = conductaRepository;
    }

    @GetMapping
    public ResponseEntity<List<ConductaConvivencia>> listarTodas() {
        return ResponseEntity.ok(conductaRepository.findAll());
    }
}
