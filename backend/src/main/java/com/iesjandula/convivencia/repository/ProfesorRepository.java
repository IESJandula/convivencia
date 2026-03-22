package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, String> {
    List<Profesor> findByEsGuardiaAndActivoTrue(Boolean esGuardia);
    List<Profesor> findByActivoTrue();
    Optional<Profesor> findByEmailAndActivoTrue(String email);

    @Query("SELECT p FROM Profesor p WHERE LOWER(TRIM(p.email)) = LOWER(TRIM(:email))")
    Optional<Profesor> findByEmailNormalized(@Param("email") String email);

    @Query("SELECT p FROM Profesor p WHERE p.activo = true OR p.activo IS NULL")
    List<Profesor> findDisponibles();
}
