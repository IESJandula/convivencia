package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.ParteExpulsion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParteExpulsionRepository extends JpaRepository<ParteExpulsion, Integer> {
    List<ParteExpulsion> findByExpulsionId(Integer expulsionId);
    List<ParteExpulsion> findByParteId(Integer parteId);
}
