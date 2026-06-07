package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando un estudiante no tiene participación registrada
 * en la convocatoria especificada.
 * <p>
 * Esta excepción debe traducirse a HTTP 404 Not Found en el controlador.
 * Se diferencia de {@link EstudianteNotFoundException} en que el estudiante sí existe
 * en la BD, pero no tiene participación en la convocatoria concreta solicitada.
 * </p>
 *
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>{@code GET /estudiantes/{id}} — el estudiante existe pero no participó en la convocatoria indicada</li>
 *   <li>{@code DELETE /estudiantes/{id}} — el estudiante existe pero no tiene participación en la convocatoria actual</li>
 * </ul>
 */
public class ParticipacionNoEncontradaException extends RuntimeException {
    public ParticipacionNoEncontradaException(Long idEstudiante, Long idConvocatoria) {
        super("El estudiante " + idEstudiante + " no tiene participación en la convocatoria " + idConvocatoria);
    }
}
