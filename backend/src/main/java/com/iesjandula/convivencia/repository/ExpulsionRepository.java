package com.iesjandula.convivencia.repository;

import com.iesjandula.convivencia.entity.Expulsion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpulsionRepository extends JpaRepository<Expulsion, Integer> {
    List<Expulsion> findByAlumnoIdAndActivoTrue(Integer alumnoId);
    List<Expulsion> findByActivoTrue();
    Page<Expulsion> findByActivoTrue(Pageable pageable);
}
