package com.cineclick.controller;

import com.cineclick.dto.ArrepentimientoRequestDTO;
import com.cineclick.dto.ArrepentimientoResponseDTO;
import com.cineclick.dto.ResumenCompraDTO;
import com.cineclick.service.AccesoAdmin;
import com.cineclick.service.ArrepentimientoService;
import com.cineclick.service.BusquedaCompraService;
import com.cineclick.service.LimitadorPorIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class ArrepentimientoController {

    private final ArrepentimientoService arrepentimientoService;
    private final BusquedaCompraService busquedaCompraService;
    private final LimitadorPorIp limitador;
    private final AccesoAdmin accesoAdmin;

    @GetMapping("/api/compras/buscar")
    public ResumenCompraDTO buscarCompra(@RequestParam String email, @RequestParam String codigo, HttpServletRequest request) {
        limitador.verificar(request);
        return busquedaCompraService.resumen(email, codigo);
    }

    @PostMapping("/api/arrepentimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public ArrepentimientoResponseDTO solicitar(@Valid @RequestBody ArrepentimientoRequestDTO dto, HttpServletRequest request) {
        limitador.verificar(request);
        return arrepentimientoService.solicitar(dto);
    }

    @GetMapping("/api/admin/arrepentimientos")
    public List<ArrepentimientoResponseDTO> listar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        accesoAdmin.verificar(usuarioId);
        return arrepentimientoService.listar();
    }

    @PatchMapping("/api/admin/arrepentimientos/{id}/aprobar")
    public ArrepentimientoResponseDTO aprobar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId, @PathVariable Long id) {
        accesoAdmin.verificar(usuarioId);
        return arrepentimientoService.aprobar(id);
    }

    @PatchMapping("/api/admin/arrepentimientos/{id}/rechazar")
    public ArrepentimientoResponseDTO rechazar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId, @PathVariable Long id) {
        accesoAdmin.verificar(usuarioId);
        return arrepentimientoService.rechazar(id);
    }
}
