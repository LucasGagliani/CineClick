package com.cineclick.controller;

import com.cineclick.dto.SoporteRequestDTO;
import com.cineclick.dto.SoporteResponseDTO;
import com.cineclick.model.EstadoConsulta;
import com.cineclick.service.AccesoAdmin;
import com.cineclick.service.LimitadorPorIp;
import com.cineclick.service.SoporteService;
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
public class SoporteController {

    private final SoporteService soporteService;
    private final LimitadorPorIp limitador;
    private final AccesoAdmin accesoAdmin;

    @PostMapping("/api/soporte")
    @ResponseStatus(HttpStatus.CREATED)
    public SoporteResponseDTO crear(@Valid @RequestBody SoporteRequestDTO dto, HttpServletRequest request) {
        limitador.verificar(request);
        return soporteService.crear(dto);
    }

    @GetMapping("/api/admin/soporte")
    public List<SoporteResponseDTO> listar(
        @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
        @RequestParam(required = false) EstadoConsulta estado
    ) {
        accesoAdmin.verificar(usuarioId);
        return soporteService.listar(estado);
    }

    @PatchMapping("/api/admin/soporte/{id}/resolver")
    public SoporteResponseDTO resolver(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId, @PathVariable Long id) {
        accesoAdmin.verificar(usuarioId);
        return soporteService.resolver(id);
    }
}
