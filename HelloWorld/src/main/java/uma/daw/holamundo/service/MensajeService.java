package uma.daw.holamundo.service;

import uma.daw.holamundo.dto.MensajeDTO;
import uma.daw.holamundo.modelo.Mensaje;
import org.springframework.stereotype.Service;

@Service
public class MensajeService {
    public MensajeDTO generarSaludo(String nombre, String ipCliente) {
        // 1. Creamos el objeto de negocio con todos los datos (IP incluida)
        Mensaje mensajeInterno = new Mensaje("¡Hola " + nombre + "!", "Sistema Central", ipCliente);

        // Simulación: aquí podríamos guardar 'mensajeInterno' en la DB (Sistema de Información)
        System.out.println("Log: Guardando mensaje desde la IP: " + mensajeInterno.getIpOrigen());

        // 2. Mapeamos manualmente al DTO para "limpiar" la respuesta
        return new MensajeDTO(mensajeInterno.getContenido(), mensajeInterno.getEmisor());
    }
}