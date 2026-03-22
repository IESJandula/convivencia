package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Imparte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImparteRepository extends JpaRepository<Imparte, Integer> {
    List<Imparte> findByProfesorEmailAndActivoTrue(String profesorEmail);
    List<Imparte> findByGrupoIdAndActivoTrue(Integer grupoId);
}
