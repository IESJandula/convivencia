package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.AvisoJefatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvisoJefaturaRepository extends JpaRepository<AvisoJefatura, Integer> {
    List<AvisoJefatura> findByActivoTrueOrderByFechaCreacionDesc();
    List<AvisoJefatura> findByLeidoFalseAndActivoTrueOrderByFechaCreacionDesc();
}
