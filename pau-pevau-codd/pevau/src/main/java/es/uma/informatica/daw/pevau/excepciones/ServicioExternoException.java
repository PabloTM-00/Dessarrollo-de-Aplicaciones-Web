package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando se produce un error al comunicarse con un servicio externo.
 * <p>
 * Esta excepción debe traducirse a HTTP 503 Service Unavailable en el controlador.
 * </p>
 * 
 * <p>Servicios externos involucrados:</p>
 * <ul>
 *   <li><b>MateriasService</b> - para obtener información de materias matriculadas</li>
 *   <li><b>SedesService</b> - para validar sedes (opcional)</li>
 * </ul>
 * 
 * <p>Causas típicas:</p>
 * <ul>
 *   <li>Timeout de conexión o lectura</li>
 *   <li>Servicio externo caído o no disponible</li>
 *   <li>Errores de red</li>
 * </ul>
 */

public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String message) {
        super(message);
    }

    public ServicioExternoException(String service, Throwable cause) {
        super("Error al llamar a " + service, cause);
    }
}