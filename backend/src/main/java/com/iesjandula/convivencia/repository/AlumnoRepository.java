package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    List<Alumno> findByActivoTrue();
    List<Alumno> findByCursoAndActivoTrue(String curso);
    List<Alumno> findByCursoAndGrupoAndActivoTrue(String curso, String grupo);

    @Query("SELECT a FROM Alumno a WHERE a.activo = true OR a.activo IS NULL")
    List<Alumno> findDisponibles();

    @Query("SELECT a FROM Alumno a WHERE (a.activo = true OR a.activo IS NULL) AND a.curso = :curso")
    List<Alumno> findByCursoDisponibles(@Param("curso") String curso);
}
