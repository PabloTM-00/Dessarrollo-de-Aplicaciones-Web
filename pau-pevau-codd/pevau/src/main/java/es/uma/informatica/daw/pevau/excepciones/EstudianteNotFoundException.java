package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando no se encuentra un estudiante con el ID especificado.
 * <p>
 * Esta excepción debe traducirse a HTTP 404 Not Found en el controlador.
 * </p>
 *
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>{@code GET /estudiantes/{id}} con ID inexistente</li>
 *   <li>{@code PUT /estudiantes/{id}} con ID inexistente</li>
 *   <li>{@code DELETE /estudiantes/{id}} con ID inexistente</li>
 * </ul>
 */
public class EstudianteNotFoundException extends RuntimeException {
    public EstudianteNotFoundException(Long id) {
        super("No existe ningún estudiante con ID: " + id);
    }
}
