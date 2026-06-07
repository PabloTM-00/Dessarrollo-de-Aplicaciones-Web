package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando una materia referenciada no existe en el servicio externo de Materias.
 * <p>
 * Esta excepción debe traducirse a HTTP 404 Not Found en el controlador.
 * Se diferencia de {@link ServicioExternoException} en que el servicio externo responde
 * correctamente pero la materia solicitada no existe (p.ej. HTTP 404 del servicio externo),
 * mientras que {@link ServicioExternoException} cubre fallos de comunicación (HTTP 503).
 * </p>
 *
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>{@code POST /estudiantes} con un ID de materia inexistente en {@code materiasMatriculadas}</li>
 *   <li>{@code PUT /estudiantes/{id}} con un ID de materia inexistente en {@code materiasMatriculadas}</li>
 * </ul>
 */
public class MateriaNotFoundException extends RuntimeException {
    public MateriaNotFoundException(Long idMateria) {
        super("No existe ninguna materia con ID: " + idMateria);
    }
}
