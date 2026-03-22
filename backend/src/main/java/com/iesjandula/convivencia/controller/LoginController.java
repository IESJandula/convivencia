package com.iesjandula.convivencia.controller;

import com.iesjandula.convivencia.dto.LoginRequestDto;
import com.iesjandula.convivencia.dto.LoginResponseDto;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    private final ProfesorRepository profesorRepository;

    public LoginController(ProfesorRepository profesorRepository) {
        this.profesorRepository = profesorRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar que el email no esté vacío
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El email es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            // Buscar el profesor en la base de datos
            Optional<Profesor> profesorOpt = profesorRepository.findByEmailNormalized(loginRequest.getEmail().trim());

            if (profesorOpt.isPresent()) {
                Profesor profesor = profesorOpt.get();
                if (Boolean.FALSE.equals(profesor.getActivo())) {
                    response.put("success", false);
                    response.put("message", "Usuario inactivo");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }

                // Crear respuesta exitosa
                LoginResponseDto loginResponse = new LoginResponseDto();
                loginResponse.setSuccess(true);
                loginResponse.setEmail(profesor.getEmail());
                loginResponse.setNombre(profesor.getNombre());
                loginResponse.setRol(profesor.getRol() != null ? profesor.getRol().name() : "PROFESOR");
                loginResponse.setEsGuardia(profesor.getEsGuardia());
                loginResponse.setActivo(!Boolean.FALSE.equals(profesor.getActivo()));
                loginResponse.setMessage("Login exitoso");

                return ResponseEntity.ok(loginResponse);
            } else {
                response.put("success", false);
                response.put("message", "Email no encontrado. Solo los profesores registrados pueden acceder.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en el servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sesión cerrada correctamente");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        Optional<Profesor> profesorOpt = profesorRepository.findByEmailNormalized(email.trim());

        if (profesorOpt.isPresent()) {
            Profesor profesor = profesorOpt.get();
            if (Boolean.FALSE.equals(profesor.getActivo())) {
                response.put("exists", false);
                return ResponseEntity.ok(response);
            }
            response.put("exists", true);
            response.put("nombre", profesor.getNombre());
            response.put("rol", profesor.getRol() != null ? profesor.getRol().name() : "PROFESOR");
            response.put("esGuardia", profesor.getEsGuardia());
            return ResponseEntity.ok(response);
        } else {
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }
    }
}
