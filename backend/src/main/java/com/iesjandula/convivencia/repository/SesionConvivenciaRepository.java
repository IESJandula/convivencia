package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.SesionConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionConvivenciaRepository extends JpaRepository<SesionConvivencia, Integer> {
    List<SesionConvivencia> findByParteIdOrderByFechaDescTramoHorarioAsc(Integer parteId);
    Optional<SesionConvivencia> findByParteIdAndFechaAndTramoHorario(Integer parteId, LocalDate fecha, String tramoHorario);
    List<SesionConvivencia> findByFechaAndTramoHorario(LocalDate fecha, String tramoHorario);
    List<SesionConvivencia> findByProfesorGuardiaEmail(String profesorEmail);
}
