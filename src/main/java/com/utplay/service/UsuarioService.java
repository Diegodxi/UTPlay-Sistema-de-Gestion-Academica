package com.utplay.service;

import com.utplay.dto.LoginRequest;
import com.utplay.dto.LoginResponse;
import com.utplay.dto.RegistroRequest;
import com.utplay.dto.UsuarioResponse;
import com.utplay.dto.UsuarioUpdateRequest;
import com.utplay.exception.BusinessException;
import com.utplay.exception.ForbiddenException;
import com.utplay.exception.ResourceNotFoundException;
import com.utplay.exception.UnauthorizedException;
import com.utplay.model.Usuario;
import com.utplay.repository.UsuarioRepository;
import com.utplay.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String correo = normalizarCorreo(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new BusinessException("Ya existe una cuenta registrada con ese correo");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setCicloActual(request.cicloActual());
        usuario.setPuntosTotales(0);
        usuario.setNivel(1);

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = normalizarCorreo(request.correo());
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.getCorreo(), usuario.getIdUsuario());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), toResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(Long idUsuario, String correoAutenticado) {
        Usuario usuario = obtenerYVerificar(idUsuario, correoAutenticado);
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(
            Long idUsuario,
            UsuarioUpdateRequest request,
            String correoAutenticado
    ) {
        Usuario usuario = obtenerYVerificar(idUsuario, correoAutenticado);
        usuario.setNombre(request.nombre().trim());
        usuario.setCicloActual(request.cicloActual());
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(normalizarCorreo(correo))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerYVerificar(Long idUsuario, String correoAutenticado) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!usuario.getCorreo().equalsIgnoreCase(correoAutenticado)) {
            throw new ForbiddenException("No puede consultar o modificar datos de otro usuario");
        }
        return usuario;
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getCicloActual(),
                usuario.getPuntosTotales(),
                usuario.getNivel(),
                usuario.getFechaRegistro()
        );
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
