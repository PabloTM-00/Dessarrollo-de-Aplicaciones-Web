package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando se viola una regla de negocio específica del dominio.
 * <p>
 * Esta excepción debe traducirse a HTTP 409 Conflict en el controlador.
 * </p>
 * 
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>Intentar cambiar {@code noEliminar} de {@code true} a {@code false}</li>
 *   <li>Intentar eliminar un instituto que tiene estudiantes asociados</li>
 *   <li>Intentar eliminar un estudiante marcado como {@code noEliminar = true}</li>
 * </ul>
 */

public class ViolacionReglaNegocioException   extends RuntimeException {
    public ViolacionReglaNegocioException(String message) {
        super(message);
    }
}