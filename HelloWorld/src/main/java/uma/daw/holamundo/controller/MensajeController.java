package uma.daw.holamundo.controller;

import uma.daw.holamundo.dto.MensajeDTO;
import uma.daw.holamundo.service.MensajeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MensajeController {

    @Autowired
    private MensajeService mensajeService;

    @GetMapping("/saludo")
    public MensajeDTO saludar(@RequestParam(defaultValue = "Invitado") String nombre, HttpServletRequest request) {
        // Obtenemos la IP de la petición HTTP para el log interno
        String ip = request.getRemoteAddr();
        return mensajeService.generarSaludo(nombre, ip);
    }
}
