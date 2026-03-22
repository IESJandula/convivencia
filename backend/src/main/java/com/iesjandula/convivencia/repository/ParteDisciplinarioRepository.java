package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.entity.ParteDisciplinario.Estado;
import com.iesjandula.convivencia.entity.ParteDisciplinario.EstadoComputo;
import com.iesjandula.convivencia.entity.ParteDisciplinario.MedidaTomada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ParteDisciplinarioRepository extends JpaRepository<ParteDisciplinario, Integer> {
    List<ParteDisciplinario> findByAlumnoIdAndActivoTrue(Integer alumnoId);
    List<ParteDisciplinario> findByProfesorEmailAndActivoTrue(String profesorEmail);
    List<ParteDisciplinario> findByEstadoAndActivoTrue(Estado estado);
    List<ParteDisciplinario> findByFechaAndActivoTrue(LocalDate fecha);
    List<ParteDisciplinario> findByActivoTrue();
        List<ParteDisciplinario> findByAlumnoCursoAndAlumnoGrupoAndActivoTrueOrderByFechaDesc(String curso, String grupo);

        @Query("""
            SELECT p FROM ParteDisciplinario p
            WHERE p.activo = true
              AND (:nombreAlumno IS NULL OR LOWER(CONCAT(p.alumno.nombre, ' ', p.alumno.apellidos)) LIKE LOWER(CONCAT('%', :nombreAlumno, '%')))
              AND (:curso IS NULL OR p.alumno.curso = :curso)
              AND (:grupo IS NULL OR p.alumno.grupo = :grupo)
            ORDER BY p.fecha DESC
            """)
        List<ParteDisciplinario> findForJefatura(
            @Param("nombreAlumno") String nombreAlumno,
            @Param("curso") String curso,
            @Param("grupo") String grupo
        );

    @Query("SELECT p FROM ParteDisciplinario p WHERE p.fecha = :fecha AND p.medidaTomada = :medida AND p.estado = :estado AND p.estadoComputo = :estadoComputo AND p.activo = true")
    List<ParteDisciplinario> findByFechaAndMedidaAndEstado(
            @Param("fecha") LocalDate fecha,
            @Param("medida") MedidaTomada medida,
            @Param("estado") Estado estado,
            @Param("estadoComputo") EstadoComputo estadoComputo
    );

        @Query("SELECT p FROM ParteDisciplinario p WHERE p.fecha = :fecha AND p.medidaTomada = :medida AND p.estado IN :estados AND p.estadoComputo = :estadoComputo AND p.activo = true")
        List<ParteDisciplinario> findByFechaAndMedidaAndEstados(
            @Param("fecha") LocalDate fecha,
            @Param("medida") MedidaTomada medida,
            @Param("estados") List<Estado> estados,
            @Param("estadoComputo") EstadoComputo estadoComputo
        );
}
