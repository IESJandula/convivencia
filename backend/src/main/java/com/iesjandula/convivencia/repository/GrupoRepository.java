package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Integer> {
    List<Grupo> findByActivoTrue();
    List<Grupo> findByCursoAndActivoTrue(String curso);
    Optional<Grupo> findByTutorEmailAndActivoTrue(String tutorEmail);
    Optional<Grupo> findByTutorEmail(String tutorEmail);
    Optional<Grupo> findFirstByTutorIsNullAndActivoTrue();
}
