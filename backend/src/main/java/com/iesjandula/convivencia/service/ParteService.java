package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.dto.ParteAulaConvivenciaDto;
import com.iesjandula.convivencia.dto.ParteRequestDto;
import com.iesjandula.convivencia.entity.*;
import com.iesjandula.convivencia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParteService {

    private final ParteDisciplinarioRepository parteRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProfesorRepository profesorRepository;
    private final ConductaConvivenciaRepository conductaRepository;

    public ParteService(ParteDisciplinarioRepository parteRepository,
                        AlumnoRepository alumnoRepository,
                        ProfesorRepository profesorRepository,
                        ConductaConvivenciaRepository conductaRepository) {
        this.parteRepository = parteRepository;
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.conductaRepository = conductaRepository;
    }

    @Transactional
    public ParteDisciplinario crearParte(ParteRequestDto dto) {
        Alumno alumno = alumnoRepository.findById(dto.getAlumnoId())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        if (!Boolean.TRUE.equals(alumno.getActivo())) {
            throw new RuntimeException("Alumno no disponible");
        }

        Profesor profesor = profesorRepository.findById(dto.getProfesorEmail())
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado"));
        if (!Boolean.TRUE.equals(profesor.getActivo())) {
            throw new RuntimeException("Profesor no disponible");
        }

        ConductaConvivencia conducta = conductaRepository.findById(dto.getConductaId())
                .orElseThrow(() -> new RuntimeException("Conducta no encontrada"));

        ParteDisciplinario parte = new ParteDisciplinario();
        parte.setProfesor(profesor);
        parte.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        parte.setCurso(dto.getCurso());
        parte.setAlumno(alumno);
        parte.setDescripcion(dto.getDescripcion());
        parte.setGravedad(dto.getGravedad() != null
                ? ParteDisciplinario.Gravedad.valueOf(dto.getGravedad())
                : ParteDisciplinario.Gravedad.LEVE);
        parte.setMedidaTomada(ParteDisciplinario.MedidaTomada.valueOf(dto.getMedidaTomada()));
        parte.setTareas(dto.getTareas());
        parte.setArchivoUrl(dto.getArchivoUrl());
        parte.setConducta(conducta);
        parte.setEstado(ParteDisciplinario.Estado.PENDIENTE);
        parte.setEstadoComputo(ParteDisciplinario.EstadoComputo.ACTIVO);
        parte.setActivo(true);

        return parteRepository.save(parte);
    }

    public List<ParteDisciplinario> listarTodos() {
        return parteRepository.findByActivoTrue();
    }

    public ParteDisciplinario obtenerPorId(Integer id) {
        ParteDisciplinario parte = parteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parte no encontrado"));
        if (!Boolean.TRUE.equals(parte.getActivo())) {
            throw new RuntimeException("Parte no encontrado");
        }
        return parte;
    }

    public List<ParteDisciplinario> listarPorProfesor(String profesorEmail) {
        return parteRepository.findByProfesorEmailAndActivoTrue(profesorEmail);
    }

    public List<ParteDisciplinario> listarPorFecha(LocalDate fecha) {
        return parteRepository.findByFechaAndActivoTrue(fecha);
    }

    public List<ParteAulaConvivenciaDto> listarPartesAulaConvivencia(LocalDate fecha) {
        List<ParteDisciplinario> partes = parteRepository.findByFechaAndMedidaAndEstados(
                fecha,
                ParteDisciplinario.MedidaTomada.AULA_CONVIVENCIA,
                List.of(ParteDisciplinario.Estado.PENDIENTE, ParteDisciplinario.Estado.EVALUADO),
                ParteDisciplinario.EstadoComputo.ACTIVO
        );

        return partes.stream().map(parte -> {
            ParteAulaConvivenciaDto dto = new ParteAulaConvivenciaDto();
            dto.setId(parte.getId());
            dto.setAlumnoId(parte.getAlumno().getId());
            dto.setAlumnoNombre(parte.getAlumno().getNombre());
            dto.setAlumnoApellidos(parte.getAlumno().getApellidos());
            dto.setCurso(parte.getAlumno().getCurso());
            dto.setGrupo(parte.getAlumno().getGrupo());
            dto.setTareas(parte.getTareas());
            dto.setArchivoUrl(parte.getArchivoUrl());
            dto.setDescripcion(parte.getDescripcion());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void eliminarParte(Integer id) {
        ParteDisciplinario parte = parteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parte no encontrado"));
        parte.setActivo(false);
        parteRepository.save(parte);
    }
}
