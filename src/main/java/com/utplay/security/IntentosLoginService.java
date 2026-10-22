package com.utplay.security;
import com.utplay.exception.ExcepcionIntentos;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntentosLoginService {
    private static final int MAX_INTENTOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(10);
    private record RegistroIntentos(int fallos, Instant bloqueadoHasta) {}
    private final ConcurrentHashMap<String, RegistroIntentos> registros = new ConcurrentHashMap<>();
    public void verificarNoBloqueado(String correo) {
        RegistroIntentos registro = registros.get(correo);
        if (registro == null || registro.bloqueadoHasta() == null) {
            return; 
        }
        Instant ahora = Instant.now();
        if (ahora.isBefore(registro.bloqueadoHasta())) {
            long segundos = Duration.between(ahora, registro.bloqueadoHasta()).getSeconds();
            long minutos = (segundos + 59) / 60; 
            throw new ExcepcionIntentos(
                    "Demasiados intentos fallidos. Intente nuevamente en " + minutos + " minuto(s)",
                    segundos);
        }
        registros.remove(correo);
    }
    public void registrarFallo(String correo) {
        registros.compute(correo, (clave, actual) -> {
            int fallos = (actual == null ? 0 : actual.fallos()) + 1;
            if (fallos >= MAX_INTENTOS) {
                return new RegistroIntentos(fallos, Instant.now().plus(DURACION_BLOQUEO));
            }
            return new RegistroIntentos(fallos, null);
        });
    }
    public void registrarExito(String correo) {
        registros.remove(correo);
    }
}