package com.cineclick.repository;

import com.cineclick.model.RolUsuario;
import com.cineclick.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    long countByRolAndActivoTrue(RolUsuario rol);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
