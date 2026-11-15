package com.cineclick.service;

import com.cineclick.dto.ClienteRequestDTO;
import com.cineclick.dto.ClienteResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.RecursoDuplicadoException;
import com.cineclick.model.Cine;
import com.cineclick.model.Cliente;
import com.cineclick.model.GeneroCliente;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Usuario;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {
    public static final int EDAD_MINIMA = 13;

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CineRepository cineRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listar() {
        return clienteRepository.findAll().stream().map(DtoMapper::toClienteResponse).toList();
    }

    @Transactional
    public ClienteResponseDTO registrar(ClienteRequestDTO dto) {
        if (!Boolean.TRUE.equals(dto.aceptaTerminos())) {
            throw new CampoInvalidoException("aceptaTerminos",
                "Tenés que aceptar los términos y condiciones y la política de privacidad");
        }
        validarEmailUnico(dto.email());
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setApellido(dto.apellido());
        usuario.setEmail(dto.email());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setAceptoTerminosEn(LocalDateTime.now(reloj));
        usuario.setContrasenaModificadaEn(LocalDateTime.now(reloj));

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        aplicarDatosPersonales(cliente, dto);
        return DtoMapper.toClienteResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return DtoMapper.toClienteResponse(buscarEntidad(id));
    }

    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = buscarEntidad(id);
        if (!cliente.getUsuario().getEmail().equalsIgnoreCase(dto.email())) {
            validarEmailUnico(dto.email());
        }
        aplicarDatosPersonales(cliente, dto);
        cliente.getUsuario().setNombre(dto.nombre());
        cliente.getUsuario().setApellido(dto.apellido());
        cliente.getUsuario().setEmail(dto.email());
        cliente.getUsuario().setPassword(passwordEncoder.encode(dto.password()));
        cliente.getUsuario().setContrasenaModificadaEn(LocalDateTime.now(reloj));
        return DtoMapper.toClienteResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarEntidad(id);
        cliente.desactivar();
        clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidad(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
    }

    public void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con ese email");
        }
    }

    private void aplicarDatosPersonales(Cliente cliente, ClienteRequestDTO dto) {
        aplicarDatosPersonales(cliente, dto.telefono(), dto.fechaNacimiento(), dto.genero(), dto.cinePreferidoId());
    }

    public void aplicarDatosPersonales(Cliente cliente, String telefono, LocalDate fechaNacimiento,
                                       GeneroCliente genero, Long cinePreferidoId) {
        String telefonoNormalizado = Normalizador.telefono(telefono, "telefono");
        if (telefonoNormalizado == null) {
            throw new CampoInvalidoException("telefono", "Ingresá tu número de teléfono");
        }
        if (fechaNacimiento == null) {
            throw new CampoInvalidoException("fechaNacimiento", "Ingresá tu fecha de nacimiento");
        }
        if (genero == null) {
            throw new CampoInvalidoException("genero", "Elegí una opción");
        }
        if (cinePreferidoId == null) {
            throw new CampoInvalidoException("cinePreferidoId", "Elegí tu complejo de preferencia");
        }
        validarEdadMinima(fechaNacimiento);
        Cine cine = buscarCineActivo(cinePreferidoId);

        cliente.setTelefono(telefonoNormalizado);
        cliente.setFechaNacimiento(fechaNacimiento);
        cliente.setGenero(genero);
        cliente.setCinePreferido(cine);
    }

    private void validarEdadMinima(LocalDate fechaNacimiento) {
        if (Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears() < EDAD_MINIMA) {
            throw new CampoInvalidoException("fechaNacimiento", "Tenés que tener al menos 13 años para crear una cuenta");
        }
    }

    private Cine buscarCineActivo(Long cineId) {
        return cineRepository.findById(cineId)
            .filter(Cine::isActivo)
            .orElseThrow(() -> new CampoInvalidoException("cinePreferidoId", "El cine elegido no existe o no está disponible"));
    }
}
