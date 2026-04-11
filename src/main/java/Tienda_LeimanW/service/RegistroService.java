/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tienda_LeimanW.service;

import Tienda_LeimanW.domain.Usuario;
import jakarta.mail.MessagingException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RegistroService {

    private final CorreoService correoService;
    private final UsuarioService usuarioService;
    private final MessageSource messageSource;

    public RegistroService(CorreoService correoService, UsuarioService usuarioService, MessageSource messageSource) {
        this.correoService = correoService;
        this.usuarioService = usuarioService;
        this.messageSource = messageSource;
    }

    //Este método se usa en el enlace del correo enviado
    public Model activar(Model model, String username, String clave) {
        Optional<Usuario> usuario = usuarioService.getUsuarioPorUsernameYPassword(username, clave);
        if (!usuario.isEmpty()) {  //Si estaba
            model.addAttribute("usuario", usuario.get());
        } else { //hay que devolver error
            model.addAttribute("titulo", messageSource.getMessage("registro.activar", null, Locale.getDefault()));
            model.addAttribute("mensaje", messageSource.getMessage("registro.activar.error", null, Locale.getDefault()));
        }
        return model;
    }

    //Este método es el que finalmente crea el usuario en el sistema
    public void activar(Usuario usuario, MultipartFile imagenFile) {
        usuario.setActivo(true);
        usuarioService.save(usuario, imagenFile, true);
    }

    public Model crearUsuario(Model model, Usuario usuario) throws MessagingException {
        String mensaje;
        try {
            String clave = demoClave();

            usuario.setPassword(clave);
            usuario.setActivo(false);

            usuarioService.save(usuario, null, false);

            String asunto = messageSource.getMessage("registro.correo.asunto", null, Locale.getDefault());
            String contenido = messageSource.getMessage("registro.correo.contenido",
                    new Object[]{usuario.getUsername(), clave},
                    Locale.getDefault());

            correoService.enviarCorreoHtml(usuario.getCorreo(), asunto, contenido);

            mensaje = messageSource.getMessage("registro.crear", null, Locale.getDefault());

        } catch (NoSuchMessageException e) {
            mensaje = "Error al crear usuario";
        }

        model.addAttribute("titulo", messageSource.getMessage("registro.crear", null, Locale.getDefault()));
        model.addAttribute("mensaje", mensaje);

        return model;
    }

    private String demoClave() {
        return "ABC123";
    }
}
