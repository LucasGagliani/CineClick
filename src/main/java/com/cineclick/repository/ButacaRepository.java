package com.cineclick.repository;

import com.cineclick.model.Butaca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ButacaRepository extends JpaRepository<Butaca, Long> {
    List<Butaca> findBySalaIdAndActivaTrue(Long salaId);
}
