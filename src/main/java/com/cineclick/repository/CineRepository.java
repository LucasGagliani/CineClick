package com.cineclick.repository;

import com.cineclick.model.Cine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CineRepository extends JpaRepository<Cine, Long> {
    List<Cine> findByActivoTrue();

    List<Cine> findByCiudadIgnoreCaseAndActivoTrue(String ciudad);
}
