package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.dto.SesionConvivenciaRequestDto;
import com.iesjandula.convivencia.entity.*;
import com.iesjandula.convivencia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class SesionConvivenciaService {

    private final SesionConvivenciaRepository sesionRepository;
    private final ParteDisciplinarioRepository parteRepository;
    private final ProfesorRepository profesorRepository;

    public SesionConvivenciaService(SesionConvivenciaRepository sesionRepository,
                                    ParteDisciplinarioRepository parteRepository,
                                    ProfesorRepository profesorRepository) {
        this.sesionRepository = sesionRepository;
        this.parteRepository = parteRepository;
        this.profesorRepository = profesorRepository;
    }

    @Transactional
    public SesionConvivencia registrarSesion(SesionConvivenciaRequestDto dto) {
        if (dto == null || dto.getParteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el parte a evaluar");
        }

        if (dto.getProfesorGuardiaEmail() == null || dto.getProfesorGuardiaEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el profesor de guardia");
        }

        ParteDisciplinario parte = parteRepository.findById(dto.getParteId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parte no encontrado"));

        String emailGuardia = dto.getProfesorGuardiaEmail().trim();
        Profesor profesorGuardia = profesorRepository.findByEmailAndActivoTrue(emailGuardia)
            .orElseGet(() -> profesorRepository.findByEmailNormalized(emailGuardia)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profesor de guardia no encontrado")));

        if (Boolean.FALSE.equals(profesorGuardia.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profesor inactivo");
        }

        LocalDate fechaSesion = dto.getFecha() != null ? dto.getFecha() : LocalDate.now();
        String tramo = normalizarTramo(dto.getTramoHorario());

        SesionConvivencia sesion = sesionRepository
            .findByParteIdAndFechaAndTramoHorario(dto.getParteId(), fechaSesion, tramo)
            .orElseGet(SesionConvivencia::new);

        sesion.setParte(parte);
        sesion.setProfesorGuardia(profesorGuardia);
        sesion.setFecha(fechaSesion);
        sesion.setTramoHorario(tramo);
        sesion.setComportamiento(parseComportamiento(dto.getComportamiento()));
        sesion.setTrabaja(dto.getTrabaja());
        sesion.setObservaciones(dto.getObservaciones());

        // Marcar parte como evaluado
        parte.setEstado(ParteDisciplinario.Estado.EVALUADO);
        parteRepository.save(parte);

        return sesionRepository.save(sesion);
    }

    public List<SesionConvivencia> listarTodas() {
        return sesionRepository.findAll();
    }

    public List<SesionConvivencia> listarPorFechaYTramo(LocalDate fecha, String tramo) {
        return sesionRepository.findByFechaAndTramoHorario(fecha, normalizarTramo(tramo));
    }

    public List<SesionConvivencia> obtenerPorParteId(Integer parteId) {
        if (parteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el parte");
        }

        return sesionRepository.findByParteIdOrderByFechaDescTramoHorarioAsc(parteId);
    }

    private String normalizarTramo(String tramo) {
        if (tramo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tramo horario inválido");
        }

        String valor = tramo.trim();
        switch (valor) {
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
                return valor;
            case "PRIMERA":
                return "1";
            case "SEGUNDA":
                return "2";
            case "TERCERA":
                return "3";
            case "CUARTA":
                return "4";
            case "QUINTA":
                return "5";
            case "SEXTA":
                return "6";
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tramo horario inválido");
        }
    }

    private SesionConvivencia.Comportamiento parseComportamiento(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }

        try {
            return SesionConvivencia.Comportamiento.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comportamiento inválido");
        }
    }
}
