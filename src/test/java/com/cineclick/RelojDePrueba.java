package com.cineclick;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

class RelojDePrueba extends Clock {

    private final ZoneId zona;
    private Instant fijo;

    RelojDePrueba(ZoneId zona) {
        this.zona = zona;
    }

    void fijar(LocalDateTime cuando) {
        fijo = cuando.atZone(zona).toInstant();
    }

    void usarHoraReal() {
        fijo = null;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        return Clock.system(otraZona);
    }

    @Override
    public Instant instant() {
        return fijo != null ? fijo : Instant.now();
    }
}
