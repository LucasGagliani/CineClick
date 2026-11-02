package com.cineclick.controller;

import com.cineclick.config.SoloAdmin;
import com.cineclick.dto.CompraAdminDTO;
import com.cineclick.dto.DetalleCompraAdminDTO;
import com.cineclick.dto.PaginaDTO;
import com.cineclick.model.EstadoCompra;
import com.cineclick.service.CompraAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin/compras")
@RequiredArgsConstructor
@SoloAdmin
public class CompraAdminController {

    private final CompraAdminService compraAdminService;

    @GetMapping
    public PaginaDTO<CompraAdminDTO> buscar(
        @RequestParam(required = false) String codigo,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Long cineId,
        @RequestParam(required = false) EstadoCompra estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "20") int tamano
    ) {
        return compraAdminService.buscar(codigo, email, cineId, estado, desde, hasta, pagina, tamano);
    }

    @GetMapping("/{id}")
    public DetalleCompraAdminDTO detalle(@PathVariable Long id) {
        return compraAdminService.detalle(id);
    }
}
