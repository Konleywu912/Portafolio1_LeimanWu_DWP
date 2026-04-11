/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tienda_LeimanW.service;

import Tienda_LeimanW.domain.Rol;
import Tienda_LeimanW.domain.Usuario;
import Tienda_LeimanW.repository.RolRepository;
import Tienda_LeimanW.repository.UsuarioRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            FirebaseStorageService firebaseStorageService,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> getUsuarios(boolean activo) {
        if (activo) {
            return usuarioRepository.findByActivoTrue();
        }
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuarioPorUsernameYPassword(String username, String password) {
        return usuarioRepository.findByUsernameAndPassword(username, password);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuarioPorUsernameOCorreo(String username, String correo) {
        return usuarioRepository.findByUsernameOrCorreo(username, correo);
    }

    @Transactional(readOnly = true)
    public boolean existeUsuarioPorUsernameOCorreo(String username, String correo) {
        return usuarioRepository.existsByUsernameOrCorreo(username, correo);
    }

    @Transactional
    public void save(Usuario usuario, MultipartFile imagenFile, boolean encriptaClave) {

        // Verifica si el correo ya existe, excluyendo el usuario actual
        final Integer idUser = usuario.getIdUsuario();
        Optional<Usuario> usuarioDuplicado = usuarioRepository.findByUsernameOrCorreo(null, usuario.getCorreo());

        if (usuarioDuplicado.isPresent()) {
            Usuario encontrado = usuarioDuplicado.get();

            if (idUser == null || !encontrado.getIdUsuario().equals(idUser)) {
                throw new DataIntegrityViolationException("El correo ya está en uso por otro usuario.");
            }
        }

        // Se valida si la clave se va a actualizar o si es un usuario nuevo
        var asignarRol = false;

        if (usuario.getIdUsuario() == null) {
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
            }

            // La primera vez como es activación no se encripta
            usuario.setPassword(
                    encriptaClave ? passwordEncoder.encode(usuario.getPassword()) : usuario.getPassword()
            );

            asignarRol = true;

        } else {
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {

                // El campo de password en el formulario viene vacío
                // Recuperamos la contraseña existente de la base de datos
                Usuario usuarioExistente = usuarioRepository.findById(usuario.getIdUsuario())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario a modificar no encontrado."));

                usuario.setPassword(
                        encriptaClave ? passwordEncoder.encode(usuarioExistente.getPassword())
                                : usuarioExistente.getPassword()
                );

            } else {
                // El campo de password NO está vacío
                usuario.setPassword(
                        encriptaClave ? passwordEncoder.encode(usuario.getPassword())
                                : usuario.getPassword()
                );
            }
        }

        usuario = usuarioRepository.save(usuario);

        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                String rutaImagen = firebaseStorageService.uploadImage(
                        imagenFile, "usuario", usuario.getIdUsuario());

                usuario.setRutaImagen(rutaImagen);
                usuarioRepository.save(usuario);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (asignarRol) {
            // Si se está creando el usuario, se crea el rol por defecto "USER"
            asignarRolPorUsername(usuario.getUsername(), "USER");
        }
    }

    @Transactional
    public void delete(Integer idUsuario) {
        // Verifica si el usuario existe antes de intentar eliminarlo
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new IllegalArgumentException(
                    "El usuario con ID " + idUsuario + " no existe.");
        }

        try {
            usuarioRepository.deleteById(idUsuario);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el usuario. Tiene datos asociados.", e);
        }
    }

    @Transactional
    public Usuario asignarRolPorUsername(String username, String rolStr) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado: " + username);
        }

        Usuario usuario = usuarioOpt.get();

        Optional<Rol> rolOpt = rolRepository.findByRol(rolStr);
        if (rolOpt.isEmpty()) {
            throw new RuntimeException("Rol no encontrado.");
        }

        Rol rol = rolOpt.get();
        usuario.getRoles().add(rol);

        return usuarioRepository.save(usuario);
    }
}
