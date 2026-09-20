package com.cineclick.repository;

import com.cineclick.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    List<Sala> findByCineIdAndActivaTrue(Long cineId);
}
