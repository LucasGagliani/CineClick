package com.cineclick.repository;

import com.cineclick.model.Butaca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ButacaRepository extends JpaRepository<Butaca, Long> {
    List<Butaca> findBySalaIdAndActivaTrue(Long salaId);

    @Query("select b.sala.id, count(b) from Butaca b where b.activa = true and b.sala.id in :salaIds group by b.sala.id")
    List<Object[]> contarActivasPorSala(@Param("salaIds") Collection<Long> salaIds);
}
