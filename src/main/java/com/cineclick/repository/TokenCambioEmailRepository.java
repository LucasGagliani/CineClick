package com.cineclick.repository;

import com.cineclick.model.TokenCambioEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenCambioEmailRepository extends JpaRepository<TokenCambioEmail, Long> {
    Optional<TokenCambioEmail> findByTokenHash(String tokenHash);

    List<TokenCambioEmail> findByUsuarioIdAndUsadoEnIsNullOrderByIdDesc(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
