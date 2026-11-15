package com.cineclick.repository;

import com.cineclick.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuarioEmailIgnoreCase(String email);

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    @Query("""
        select c from Cliente c join fetch c.usuario u
        where (lower(u.email) like :sufijo1 or lower(u.email) like :sufijo2)
          and (c.fechaNacimiento is null or c.genero is null or c.cinePreferido is null)
        order by c.id
        """)
    List<Cliente> findIncompletosConEmailTerminadoEn(@Param("sufijo1") String sufijo1, @Param("sufijo2") String sufijo2);
}
