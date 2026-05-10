package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.TareaExpulsion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaExpulsionRepository extends JpaRepository<TareaExpulsion, Integer> {
    List<TareaExpulsion> findByProfesorEmail(String profesorEmail);
    Page<TareaExpulsion> findByProfesorEmail(String profesorEmail, Pageable pageable);
    Page<TareaExpulsion> findByProfesorEmailAndEstado(String profesorEmail, TareaExpulsion.Estado estado, Pageable pageable);
    List<TareaExpulsion> findByExpulsionId(Integer expulsionId);
    List<TareaExpulsion> findByProfesorEmailAndEstado(String profesorEmail, TareaExpulsion.Estado estado);
    long countByProfesorEmail(String profesorEmail);
    long countByProfesorEmailAndEstado(String profesorEmail, TareaExpulsion.Estado estado);
}
