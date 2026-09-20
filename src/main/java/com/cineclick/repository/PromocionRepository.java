package com.cineclick.repository;

import com.cineclick.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    Optional<Promocion> findByCodigoIgnoreCase(String codigo);

    List<Promocion> findByActivaTrue();
}
