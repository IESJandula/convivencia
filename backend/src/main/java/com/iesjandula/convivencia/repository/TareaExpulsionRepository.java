package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.TareaExpulsion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaExpulsionRepository extends JpaRepository<TareaExpulsion, Integer> {
    List<TareaExpulsion> findByProfesorEmail(String profesorEmail);
    List<TareaExpulsion> findByExpulsionId(Integer expulsionId);
    List<TareaExpulsion> findByProfesorEmailAndEstado(String profesorEmail, TareaExpulsion.Estado estado);
}
