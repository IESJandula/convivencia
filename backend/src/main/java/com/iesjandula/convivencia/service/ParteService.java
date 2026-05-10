package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.dto.ParteAulaConvivenciaDto;
import com.iesjandula.convivencia.dto.ParteRequestDto;
import com.iesjandula.convivencia.entity.*;
import com.iesjandula.convivencia.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParteService {

    private final ParteDisciplinarioRepository parteRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProfesorRepository profesorRepository;
    private final ConductaConvivenciaRepository conductaRepository;
    private final AvisoJefaturaRepository avisoJefaturaRepository;
    private final ExpulsionRepository expulsionRepository;

    public ParteService(ParteDisciplinarioRepository parteRepository,
                        AlumnoRepository alumnoRepository,
                        ProfesorRepository profesorRepository,
                        ConductaConvivenciaRepository conductaRepository,
                        AvisoJefaturaRepository avisoJefaturaRepository,
                        ExpulsionRepository expulsionRepository) {
        this.parteRepository = parteRepository;
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.conductaRepository = conductaRepository;
        this.avisoJefaturaRepository = avisoJefaturaRepository;
        this.expulsionRepository = expulsionRepository;
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
        parte.setCurso(alumno.getGrupo() != null ? alumno.getGrupo().getCurso() : "Sin curso");
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

        ParteDisciplinario guardado = parteRepository.save(parte);

        // Lógica de avisos a Jefatura
        if (guardado.getGravedad() == ParteDisciplinario.Gravedad.LEVE && guardado.getActivo()) {
            long partesLeves = parteRepository.countByAlumnoIdAndGravedadAndEstadoComputoAndActivoTrue(alumno.getId(), ParteDisciplinario.Gravedad.LEVE, ParteDisciplinario.EstadoComputo.ACTIVO);
            if (partesLeves >= 3) {
                var avisoPendienteOpt = avisoJefaturaRepository.findByLeidoFalseAndActivoTrueOrderByFechaCreacionDesc()
                        .stream()
                        .filter(a -> a.getAlumno().getId().equals(alumno.getId()))
                        .findFirst();
                
                if (avisoPendienteOpt.isPresent()) {
                    AvisoJefatura aviso = avisoPendienteOpt.get();
                    aviso.setMensaje("El alumno " + alumno.getNombre() + " " + alumno.getApellidos() + " ha acumulado " + partesLeves + " partes leves.");
                    avisoJefaturaRepository.save(aviso);
                } else {
                    AvisoJefatura aviso = new AvisoJefatura();
                    aviso.setAlumno(alumno);
                    aviso.setMensaje("El alumno " + alumno.getNombre() + " " + alumno.getApellidos() + " ha acumulado " + partesLeves + " partes leves.");
                    avisoJefaturaRepository.save(aviso);
                }
            }
        }

        return guardado;
    }

    public List<ParteDisciplinario> listarTodos() {
        return parteRepository.findByActivoTrue();
    }

    public Page<ParteDisciplinario> listarHistorialPaginado(String profesorEmail,
                                                            LocalDate fechaDesde,
                                                            LocalDate fechaHasta,
                                                            String alumnoTexto,
                                                            String profesorTexto,
                                                            String gravedad,
                                                            String conductaTexto,
                                                            String estadoComputo,
                                                            Integer alumnoId,
                                                            Pageable pageable) {
        Specification<ParteDisciplinario> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("activo")));

            if (profesorEmail != null && !profesorEmail.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("profesor").get("email")), profesorEmail.toLowerCase()));
            }

            if (alumnoId != null) {
                predicates.add(cb.equal(root.get("alumno").get("id"), alumnoId));
            }

            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            }

            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }

            if (alumnoTexto != null && !alumnoTexto.isBlank()) {
                String pattern = "%" + alumnoTexto.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("alumno").get("nombre")), pattern),
                        cb.like(cb.lower(root.get("alumno").get("apellidos")), pattern),
                        cb.like(cb.lower(root.get("alumno").get("grupo").get("curso")), pattern),
                        cb.like(cb.lower(root.get("alumno").get("grupo").get("letra")), pattern)
                ));
            }

            if (profesorTexto != null && !profesorTexto.isBlank()) {
                String pattern = "%" + profesorTexto.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("profesor").get("nombre")), pattern));
            }

            if (gravedad != null && !gravedad.isBlank()) {
                try {
                    ParteDisciplinario.Gravedad gravedadEnum = ParteDisciplinario.Gravedad.valueOf(gravedad);
                    predicates.add(cb.equal(root.get("gravedad"), gravedadEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (conductaTexto != null && !conductaTexto.isBlank()) {
                String pattern = "%" + conductaTexto.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("conducta").get("codigo")), pattern),
                        cb.like(cb.lower(root.get("conducta").get("descripcion")), pattern)
                ));
            }

            if (estadoComputo != null && !estadoComputo.isBlank()) {
                if ("COMPUTADO".equalsIgnoreCase(estadoComputo)) {
                    predicates.add(cb.equal(root.get("estadoComputo"), ParteDisciplinario.EstadoComputo.COMPUTADO));
                } else if ("PENDIENTE".equalsIgnoreCase(estadoComputo)) {
                    predicates.add(cb.equal(root.get("estadoComputo"), ParteDisciplinario.EstadoComputo.ACTIVO));
                } else {
                    try {
                        ParteDisciplinario.EstadoComputo estadoEnum = ParteDisciplinario.EstadoComputo.valueOf(estadoComputo);
                        predicates.add(cb.equal(root.get("estadoComputo"), estadoEnum));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return parteRepository.findAll(spec, pageable);
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
        List<ParteDisciplinario> partes = parteRepository.findPendientesSinSesionAula(
            ParteDisciplinario.MedidaTomada.AULA_CONVIVENCIA,
            ParteDisciplinario.Estado.PENDIENTE,
            ParteDisciplinario.EstadoComputo.ACTIVO
        );

        return partes.stream().map(this::toAulaConvivenciaDto).collect(Collectors.toList());
    }

    public Page<ParteAulaConvivenciaDto> listarPartesAulaConvivenciaPaginado(LocalDate fecha, Pageable pageable) {
        Page<ParteDisciplinario> partes = parteRepository.findPendientesSinSesionAulaPaginado(
            ParteDisciplinario.MedidaTomada.AULA_CONVIVENCIA,
            ParteDisciplinario.Estado.PENDIENTE,
            ParteDisciplinario.EstadoComputo.ACTIVO,
            pageable
        );

        return partes.map(this::toAulaConvivenciaDto);
    }

    private ParteAulaConvivenciaDto toAulaConvivenciaDto(ParteDisciplinario parte) {
        ParteAulaConvivenciaDto dto = new ParteAulaConvivenciaDto();
        dto.setId(parte.getId());
        dto.setAlumnoId(parte.getAlumno().getId());
        dto.setAlumnoNombre(parte.getAlumno().getNombre());
        dto.setAlumnoApellidos(parte.getAlumno().getApellidos());
        dto.setCurso(parte.getAlumno().getGrupo() != null ? parte.getAlumno().getGrupo().getCurso() : "");
        dto.setGrupo(parte.getAlumno().getGrupo() != null ? parte.getAlumno().getGrupo().getLetra() : "");
        dto.setTareas(parte.getTareas());
        dto.setArchivoUrl(parte.getArchivoUrl());
        dto.setDescripcion(parte.getDescripcion());
        return dto;
    }

    @Transactional
    public void eliminarParte(Integer id) {
        ParteDisciplinario parte = parteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parte no encontrado"));
        parte.setActivo(false);
        parteRepository.save(parte);
    }
}
