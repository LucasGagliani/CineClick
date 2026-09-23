package com.cineclick.service;

import com.cineclick.dto.ClienteRequestDTO;
import com.cineclick.dto.ClienteResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.RecursoDuplicadoException;
import com.cineclick.model.Cliente;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Usuario;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listar() {
        return clienteRepository.findAll().stream().map(DtoMapper::toClienteResponse).toList();
    }

    @Transactional
    public ClienteResponseDTO registrar(ClienteRequestDTO dto) {
        validarEmailUnico(dto.email());
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setApellido(dto.apellido());
        usuario.setEmail(dto.email());
        usuario.setPassword(dto.password());
        usuario.setRol(RolUsuario.CLIENTE);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setTelefono(dto.telefono());
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
        cliente.getUsuario().setNombre(dto.nombre());
        cliente.getUsuario().setApellido(dto.apellido());
        cliente.getUsuario().setEmail(dto.email());
        cliente.getUsuario().setPassword(dto.password());
        cliente.setTelefono(dto.telefono());
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
            throw new RecursoDuplicadoException("Ya existe un usuario registrado con el email " + email);
        }
    }
}
