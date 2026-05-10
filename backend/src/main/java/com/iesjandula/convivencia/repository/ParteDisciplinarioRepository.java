package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.entity.ParteDisciplinario.Estado;
import com.iesjandula.convivencia.entity.ParteDisciplinario.EstadoComputo;
import com.iesjandula.convivencia.entity.ParteDisciplinario.MedidaTomada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ParteDisciplinarioRepository extends JpaRepository<ParteDisciplinario, Integer>, JpaSpecificationExecutor<ParteDisciplinario> {
    List<ParteDisciplinario> findByAlumnoIdAndActivoTrue(Integer alumnoId);
    List<ParteDisciplinario> findByProfesorEmailAndActivoTrue(String profesorEmail);
    List<ParteDisciplinario> findByEstadoAndActivoTrue(Estado estado);
    List<ParteDisciplinario> findByFechaAndActivoTrue(LocalDate fecha);
    List<ParteDisciplinario> findByActivoTrue();
    Page<ParteDisciplinario> findByActivoTrue(Pageable pageable);
    Page<ParteDisciplinario> findByProfesorEmailAndActivoTrue(String profesorEmail, Pageable pageable);
    List<ParteDisciplinario> findByAlumnoGrupoCursoAndAlumnoGrupoLetraAndActivoTrueOrderByFechaDesc(String curso, String grupo);

    long countByAlumnoIdAndGravedadAndActivoTrue(Integer alumnoId, ParteDisciplinario.Gravedad gravedad);
    long countByAlumnoIdAndGravedadAndEstadoComputoAndActivoTrue(Integer alumnoId, ParteDisciplinario.Gravedad gravedad, EstadoComputo estadoComputo);


        @Query("""
            SELECT p FROM ParteDisciplinario p
            WHERE p.activo = true
              AND (:nombreAlumno IS NULL OR LOWER(CONCAT(p.alumno.nombre, ' ', p.alumno.apellidos)) LIKE LOWER(CONCAT('%', :nombreAlumno, '%')))
              AND (:curso IS NULL OR p.alumno.grupo.curso = :curso)
              AND (:grupo IS NULL OR p.alumno.grupo.letra = :grupo)
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

        @Query("SELECT p FROM ParteDisciplinario p WHERE p.medidaTomada = :medida AND p.estado = :estado AND p.estadoComputo = :estadoComputo AND p.activo = true ORDER BY p.fecha DESC, p.id DESC")
        List<ParteDisciplinario> findByMedidaAndEstadoAndEstadoComputo(
            @Param("medida") MedidaTomada medida,
            @Param("estado") Estado estado,
            @Param("estadoComputo") EstadoComputo estadoComputo
        );

        @Query("""
            SELECT p FROM ParteDisciplinario p
            WHERE p.medidaTomada = :medida
              AND p.estado = :estado
              AND p.estadoComputo = :estadoComputo
              AND p.activo = true
              AND NOT EXISTS (
                    SELECT s.id FROM SesionConvivencia s
                    WHERE s.parte.id = p.id
              )
            ORDER BY p.fecha DESC, p.id DESC
        """)
        List<ParteDisciplinario> findPendientesSinSesionAula(
            @Param("medida") MedidaTomada medida,
            @Param("estado") Estado estado,
            @Param("estadoComputo") EstadoComputo estadoComputo
        );

        @Query(
            value = """
                SELECT p FROM ParteDisciplinario p
                WHERE p.medidaTomada = :medida
                  AND p.estado = :estado
                  AND p.estadoComputo = :estadoComputo
                  AND p.activo = true
                  AND (:curso IS NULL OR p.alumno.grupo.curso = :curso)
                  AND (:grupo IS NULL OR p.alumno.grupo.letra = :grupo)
                  AND NOT EXISTS (
                        SELECT s.id FROM SesionConvivencia s
                        WHERE s.parte.id = p.id
                  )
                ORDER BY p.fecha DESC, p.id DESC
            """,
            countQuery = """
                SELECT COUNT(p) FROM ParteDisciplinario p
                WHERE p.medidaTomada = :medida
                  AND p.estado = :estado
                  AND p.estadoComputo = :estadoComputo
                  AND p.activo = true
                  AND (:curso IS NULL OR p.alumno.grupo.curso = :curso)
                  AND (:grupo IS NULL OR p.alumno.grupo.letra = :grupo)
                  AND NOT EXISTS (
                        SELECT s.id FROM SesionConvivencia s
                        WHERE s.parte.id = p.id
                  )
            """
        )
        Page<ParteDisciplinario> findPendientesSinSesionAulaPaginado(
            @Param("medida") MedidaTomada medida,
            @Param("estado") Estado estado,
            @Param("estadoComputo") EstadoComputo estadoComputo,
            @Param("curso") String curso,
            @Param("grupo") String grupo,
            Pageable pageable
        );

            @Query("SELECT p FROM ParteDisciplinario p WHERE p.medidaTomada = :medida AND p.estadoComputo = :estadoComputo AND p.activo = true ORDER BY p.fecha DESC, p.id DESC")
            List<ParteDisciplinario> findByMedidaAndEstadoComputo(
                @Param("medida") MedidaTomada medida,
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
