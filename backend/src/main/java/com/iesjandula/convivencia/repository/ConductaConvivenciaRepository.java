package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.ConductaConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConductaConvivenciaRepository extends JpaRepository<ConductaConvivencia, Integer> {
	List<ConductaConvivencia> findAllByOrderByCodigoAsc();
	List<ConductaConvivencia> findByGravedadOrderByCodigoAsc(ConductaConvivencia.Gravedad gravedad);
}
