package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.dto.TareaExpulsionDto;
import com.iesjandula.convivencia.entity.TareaExpulsion;
import com.iesjandula.convivencia.repository.TareaExpulsionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TareaExpulsionService {
    private static final String TEXTO_TAREA_AUTOGENERADA = "tarea pendiente por expulsión del alumno";

    private final TareaExpulsionRepository tareaExpulsionRepository;

    public TareaExpulsionService(TareaExpulsionRepository tareaExpulsionRepository) {
        this.tareaExpulsionRepository = tareaExpulsionRepository;
    }

    public List<TareaExpulsionDto> listarPorProfesor(String profesorEmail) {
        return tareaExpulsionRepository.findByProfesorEmail(profesorEmail).stream()
                .sorted(Comparator
                        .comparing(TareaExpulsion::getEstado)
                        .thenComparing(TareaExpulsion::getId, Comparator.reverseOrder()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TareaExpulsionDto> listarPorExpulsion(Integer expulsionId) {
        if (expulsionId == null) {
            throw new RuntimeException("Debe indicar la expulsión");
        }

        return tareaExpulsionRepository.findByExpulsionId(expulsionId).stream()
                .sorted(Comparator.comparing(TareaExpulsion::getAsignatura, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TareaExpulsionDto marcarCompletada(Integer tareaId, String profesorEmail) {
        return actualizarTareaProfesor(tareaId, profesorEmail, null, true);
    }

    @Transactional
    public TareaExpulsionDto actualizarTareaProfesor(Integer tareaId,
                                                     String profesorEmail,
                                                     String descripcionTarea,
                                                     Boolean completar) {
        if (profesorEmail == null || profesorEmail.trim().isEmpty()) {
            throw new RuntimeException("Debe indicar el profesor");
        }

        TareaExpulsion tarea = tareaExpulsionRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (!tarea.getProfesor().getEmail().equalsIgnoreCase(profesorEmail)) {
            throw new RuntimeException("No puedes modificar tareas de otro profesor");
        }

        if (descripcionTarea != null) {
            String descripcionLimpia = descripcionTarea.trim();
            if (!descripcionLimpia.isEmpty()) {
                tarea.setDescripcionTarea(descripcionLimpia);
            }
        }

        if (Boolean.TRUE.equals(completar)) {
            if (!esActividadProfesorValida(tarea.getDescripcionTarea())) {
                throw new RuntimeException("Debes escribir una actividad real antes de enviarla");
            }
            tarea.setEstado(TareaExpulsion.Estado.COMPLETADA);
        }

        tarea = tareaExpulsionRepository.save(tarea);
        return toDto(tarea);
    }

    private TareaExpulsionDto toDto(TareaExpulsion tarea) {
        String alumnoNombreCompleto = tarea.getExpulsion().getAlumno().getNombre() + " " +
                tarea.getExpulsion().getAlumno().getApellidos();

        return new TareaExpulsionDto(
                tarea.getId(),
                tarea.getExpulsion().getId(),
                tarea.getProfesor().getEmail(),
                tarea.getProfesor().getNombre(),
                alumnoNombreCompleto,
                tarea.getExpulsion().getFechaInicio(),
                tarea.getExpulsion().getFechaFin(),
                tarea.getAsignatura(),
                tarea.getDescripcionTarea(),
                tarea.getEstado().name()
        );
    }

    private boolean esActividadProfesorValida(String descripcionTarea) {
        if (descripcionTarea == null) {
            return false;
        }

        String texto = descripcionTarea.trim();
        if (texto.isEmpty()) {
            return false;
        }

        return !texto.toLowerCase().startsWith(TEXTO_TAREA_AUTOGENERADA);
    }
}
