package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.ConductaConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConductaConvivenciaRepository extends JpaRepository<ConductaConvivencia, Integer> {
}
