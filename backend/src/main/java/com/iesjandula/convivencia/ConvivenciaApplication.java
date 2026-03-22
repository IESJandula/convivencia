package com.iesjandula.convivencia;

import com.iesjandula.convivencia.entity.Alumno;
import com.iesjandula.convivencia.entity.Grupo;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.AlumnoRepository;
import com.iesjandula.convivencia.repository.GrupoRepository;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class ConvivenciaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConvivenciaApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner seedProfesores(ProfesorRepository profesorRepository) {
        return args -> {
            long total = profesorRepository.count();
            long activos = profesorRepository.findAll().stream()
                    .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                    .count();

            if (total > 0 && activos == 0) {
                List<Profesor> profesores = profesorRepository.findAll();
                for (Profesor p : profesores) {
                    p.setActivo(true);
                }
                profesorRepository.saveAll(profesores);
                return;
            }

            if (total > 0) {
                return;
            }

            Profesor profesor = new Profesor();
            profesor.setEmail("profesor@g.educaand.es");
            profesor.setNombre("Profesor Demo");
            profesor.setRol(Profesor.Rol.PROFESOR);
            profesor.setEsGuardia(false);
            profesor.setActivo(true);

            Profesor tutor = new Profesor();
            tutor.setEmail("tutor@g.educaand.es");
            tutor.setNombre("Tutor Demo");
            tutor.setRol(Profesor.Rol.TUTOR);
            tutor.setEsGuardia(false);
            tutor.setActivo(true);

            Profesor jefatura = new Profesor();
            jefatura.setEmail("jefatura@g.educaand.es");
            jefatura.setNombre("Jefatura Demo");
            jefatura.setRol(Profesor.Rol.JEFATURA);
            jefatura.setEsGuardia(false);
            jefatura.setActivo(true);

            Profesor guardia = new Profesor();
            guardia.setEmail("guardia@g.educaand.es");
            guardia.setNombre("Guardia Demo");
            guardia.setRol(Profesor.Rol.PROFESOR);
            guardia.setEsGuardia(true);
            guardia.setActivo(true);

            profesorRepository.saveAll(List.of(profesor, tutor, jefatura, guardia));
        };
    }

    @Bean
    public org.springframework.boot.CommandLineRunner backfillAlumnosActivos(AlumnoRepository alumnoRepository) {
        return args -> {
            long total = alumnoRepository.count();
            if (total == 0) {
                return;
            }

            long activos = alumnoRepository.findAll().stream()
                    .filter(a -> !Boolean.FALSE.equals(a.getActivo()))
                    .count();

            if (activos == 0) {
                List<Alumno> alumnos = alumnoRepository.findAll();
                for (Alumno a : alumnos) {
                    a.setActivo(true);
                }
                alumnoRepository.saveAll(alumnos);
            }
        };
    }

    @Bean
    public org.springframework.boot.CommandLineRunner asignarTutoriasAleatorias(
            ProfesorRepository profesorRepository,
            AlumnoRepository alumnoRepository,
            GrupoRepository grupoRepository
    ) {
        return args -> {
                List<Profesor> profesoresTutoria = new ArrayList<>(profesorRepository.findByActivoTrue().stream()
                    .filter(p -> p.getEmail() != null)
                    .filter(p -> p.getRol() == Profesor.Rol.PROFESOR || p.getRol() == Profesor.Rol.TUTOR)
                    .toList());

            if (profesoresTutoria.isEmpty()) {
                return;
            }

            List<Grupo> grupos = new ArrayList<>(grupoRepository.findByActivoTrue());
            Set<String> combinacionesReales = new LinkedHashSet<>();
            for (Alumno alumno : alumnoRepository.findByActivoTrue()) {
                String curso = alumno.getCurso() == null ? null : alumno.getCurso().trim();
                String letra = alumno.getGrupo() == null ? null : alumno.getGrupo().trim().toUpperCase(Locale.ROOT);

                if (curso == null || curso.isBlank() || letra == null || letra.isBlank()) {
                    continue;
                }
                combinacionesReales.add(curso + "|" + letra);
            }

            // Si no hay grupos cargados, se crean a partir de curso+grupo de alumnado activo.
            if (grupos.isEmpty()) {
                for (String combinacion : combinacionesReales) {
                    String[] partes = combinacion.split("\\|", 2);
                    Grupo nuevo = new Grupo();
                    nuevo.setCurso(partes[0]);
                    nuevo.setLetra(partes[1]);
                    nuevo.setActivo(true);
                    nuevo.setTutor(null);
                    grupos.add(nuevo);
                }

                if (!grupos.isEmpty()) {
                    grupos = new ArrayList<>(grupoRepository.saveAll(grupos));
                }
            }

            Map<String, Grupo> grupoUnicoPorCurso = new HashMap<>();
            List<Grupo> gruposDuplicados = new ArrayList<>();
            for (Grupo grupo : grupos) {
                String clave = claveCursoGrupo(grupo.getCurso(), grupo.getLetra());
                Grupo existente = grupoUnicoPorCurso.get(clave);
                if (existente == null) {
                    grupoUnicoPorCurso.put(clave, grupo);
                } else {
                    gruposDuplicados.add(grupo);
                }
            }

            if (!gruposDuplicados.isEmpty()) {
                for (Grupo duplicado : gruposDuplicados) {
                    duplicado.setTutor(null);
                    duplicado.setActivo(false);
                }
                grupoRepository.saveAll(gruposDuplicados);
            }

            grupos = new ArrayList<>(grupoUnicoPorCurso.values());

            if (grupos.isEmpty()) {
                return;
            }

            List<String> opcionesReales = new ArrayList<>(combinacionesReales);
            Collections.shuffle(opcionesReales);

            for (String opcion : opcionesReales) {
                if (grupoUnicoPorCurso.containsKey(opcion)) {
                    continue;
                }

                String[] partes = opcion.split("\\|", 2);
                Grupo nuevo = new Grupo();
                nuevo.setCurso(partes[0]);
                nuevo.setLetra(partes[1]);
                nuevo.setActivo(true);
                nuevo.setTutor(null);
                Grupo guardado = grupoRepository.save(nuevo);
                grupoUnicoPorCurso.put(opcion, guardado);
                grupos.add(guardado);

                if (grupos.size() >= profesoresTutoria.size()) {
                    break;
                }
            }

            Collections.shuffle(profesoresTutoria);
            Collections.shuffle(grupos);

            // Se elimina cualquier referencia previa de tutor para evitar choques por índice único.
            for (Profesor profesor : profesoresTutoria) {
                grupoRepository.findByTutorEmail(profesor.getEmail()).ifPresent(grupoAnterior -> {
                    grupoAnterior.setTutor(null);
                    grupoRepository.save(grupoAnterior);
                });
            }

            // Se limpian asignaciones previas para repartir de nuevo de forma aleatoria.
            for (Grupo grupo : grupos) {
                grupo.setTutor(null);
            }

            for (int i = 0; i < profesoresTutoria.size(); i++) {
                if (i >= grupos.size()) {
                    break;
                }
                grupos.get(i).setTutor(profesoresTutoria.get(i));
            }

            grupoRepository.saveAll(grupos);
        };
    }

    private String claveCursoGrupo(String curso, String letra) {
        String c = curso == null ? "" : curso.trim().toUpperCase(Locale.ROOT);
        String l = letra == null ? "" : letra.trim().toUpperCase(Locale.ROOT);
        return c + "|" + l;
    }
}
