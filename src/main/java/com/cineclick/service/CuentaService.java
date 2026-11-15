package com.cineclick.service;

import com.cineclick.dto.CuentaResponseDTO;
import com.cineclick.dto.DatosPersonalesRequestDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.ConsultaSoporte;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.SolicitudArrepentimiento;
import com.cineclick.model.TokenCambioEmail;
import com.cineclick.model.Usuario;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.ConsultaSoporteRepository;
import com.cineclick.repository.ListaEsperaRepository;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import com.cineclick.repository.TokenCambioEmailRepository;
import com.cineclick.repository.TokenRecuperacionRepository;
import com.cineclick.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CuentaService {

    public static final String NOMBRE_ANONIMO = "Usuario";
    public static final String APELLIDO_ANONIMO = "eliminado";

    private final SesionUsuario sesion;
    private final VerificadorDeContrasena verificador;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final CompraRepository compraRepository;
    private final SolicitudArrepentimientoRepository arrepentimientoRepository;
    private final ConsultaSoporteRepository consultaRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final TokenCambioEmailRepository tokenCambioEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public CuentaResponseDTO ver(HttpServletRequest request) {
        return armar(sesion.actual(request));
    }

    @Transactional
    public CuentaResponseDTO actualizarDatos(DatosPersonalesRequestDTO dto, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        String nombre = Normalizador.texto(dto.nombre());
        String apellido = Normalizador.texto(dto.apellido());
        clienteRepository.findByUsuarioId(usuario.getId()).ifPresent(cliente ->
            clienteService.aplicarDatosPersonales(cliente, dto.telefono(), dto.fechaNacimiento(), dto.genero(), dto.cinePreferidoId()));
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return armar(usuario);
    }

    @Transactional
    public CuentaResponseDTO cambiarContrasena(String actual, String nueva, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        verificador.verificar(usuario, actual, "actual", request);
        if (passwordEncoder.matches(nueva, usuario.getPassword())) {
            throw new CampoInvalidoException("nueva", "La contraseña nueva tiene que ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
        usuario.setContrasenaModificadaEn(LocalDateTime.now(reloj));
        tokenRecuperacionRepository.deleteByUsuarioId(usuario.getId());
        return armar(usuario);
    }

    @Transactional
    public CuentaResponseDTO cambiarComunicaciones(boolean recibirNovedades, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        usuario.setRecibirNovedades(recibirNovedades);
        return armar(usuario);
    }

    @Transactional
    public void eliminar(String contrasena, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        verificador.verificar(usuario, contrasena, "contrasena", request);
        if (usuario.getRol() == RolUsuario.ADMIN && usuarioRepository.countByRolAndActivoTrue(RolUsuario.ADMIN) <= 1) {
            throw new ReglaNegocioException("No podés eliminar la última cuenta de administrador");
        }

        String emailOriginal = usuario.getEmail();
        String emailAnonimo = "eliminado-" + usuario.getId() + "@cineclick.invalid";
        Cliente cliente = clienteRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (cliente != null) {
            compraRepository.findByClienteIdAndEstado(cliente.getId(), EstadoCompra.PENDIENTE_PAGO).forEach(Compra::cancelar);
            for (SolicitudArrepentimiento solicitud : arrepentimientoRepository.findByCompraClienteIdOrderByFechaDesc(cliente.getId())) {
                solicitud.setNombre(NOMBRE_ANONIMO + " " + APELLIDO_ANONIMO);
                solicitud.setEmail(emailAnonimo);
            }
            listaEsperaRepository.deleteByClienteId(cliente.getId());
            cliente.setTelefono(null);
            cliente.setFechaNacimiento(null);
            cliente.setGenero(null);
            cliente.setCinePreferido(null);
        }
        for (ConsultaSoporte consulta : consultaRepository.buscarDelUsuario(emailOriginal, cliente == null ? null : cliente.getId())) {
            consulta.setNombre(NOMBRE_ANONIMO + " " + APELLIDO_ANONIMO);
            consulta.setEmail(emailAnonimo);
        }
        tokenRecuperacionRepository.deleteByUsuarioId(usuario.getId());
        tokenCambioEmailRepository.deleteByUsuarioId(usuario.getId());

        usuario.setNombre(NOMBRE_ANONIMO);
        usuario.setApellido(APELLIDO_ANONIMO);
        usuario.setEmail(emailAnonimo);
        usuario.setPassword(passwordEncoder.encode(TokensSeguros.generar()));
        usuario.setRecibirNovedades(false);
        usuario.desactivar();
    }

    @Transactional(readOnly = true)
    public CuentaResponseDTO armar(Usuario usuario) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuario.getId()).orElse(null);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        TokenCambioEmail pendiente = tokenCambioEmailRepository.findByUsuarioIdAndUsadoEnIsNullOrderByIdDesc(usuario.getId()).stream()
            .filter(t -> t.vigenteEn(ahora))
            .findFirst()
            .orElse(null);
        return new CuentaResponseDTO(
            usuario.getId(),
            cliente == null ? null : cliente.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.nombreCompleto(),
            usuario.getEmail(),
            usuario.getRol(),
            cliente == null ? null : cliente.getFechaRegistro(),
            cliente == null ? null : cliente.getTelefono(),
            cliente == null ? null : cliente.getFechaNacimiento(),
            cliente == null ? null : cliente.getGenero(),
            cliente == null || cliente.getCinePreferido() == null ? null : cliente.getCinePreferido().getId(),
            cliente == null || cliente.getCinePreferido() == null ? null : cliente.getCinePreferido().getNombre(),
            usuario.getContrasenaModificadaEn(),
            usuario.quiereNovedades(),
            pendiente == null ? null : pendiente.getEmailNuevo(),
            pendiente == null ? null : pendiente.getExpiraEn()
        );
    }
}
