package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Expulsion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpulsionRepository extends JpaRepository<Expulsion, Integer> {
    List<Expulsion> findByAlumnoIdAndActivoTrue(Integer alumnoId);
    List<Expulsion> findByActivoTrue();
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Expulsion e " +
           "JOIN e.alumno a " +
           "JOIN a.grupo g " +
           "WHERE e.activo = true " +
           "AND (:curso IS NULL OR g.curso = :curso) " +
           "AND (:letra IS NULL OR g.letra = :letra)")
    Page<Expulsion> findByFilters(String curso, String letra, Pageable pageable);
}
