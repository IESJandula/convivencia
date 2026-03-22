package com.iesjandula.convivencia;

import com.iesjandula.convivencia.entity.Alumno;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.AlumnoRepository;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

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
}
