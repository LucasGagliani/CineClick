package com.cineclick.repository;

import com.cineclick.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    Optional<Promocion> findByCodigoIgnoreCase(String codigo);

    List<Promocion> findByActivaTrue();

    List<Promocion> findAllByOrderByVigenciaHastaDesc();

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE promociones SET usos_actuales = usos_actuales + 1 "
        + "WHERE id = :id AND (usos_maximos_totales IS NULL OR usos_actuales < usos_maximos_totales)", nativeQuery = true)
    int incrementarUso(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE promociones SET usos_actuales = usos_actuales - 1 WHERE id = :id AND usos_actuales > 0", nativeQuery = true)
    int decrementarUso(@Param("id") Long id);
}
