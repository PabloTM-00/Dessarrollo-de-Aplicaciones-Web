package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando se necesita la convocatoria actual pero el servicio
 * externo de Convocatorias no devuelve ninguna convocatoria activa.
 * <p>
 * Esta excepción debe traducirse a HTTP 503 Service Unavailable en el controlador,
 * ya que sin convocatoria actual no es posible completar la operación solicitada.
 * </p>
 *
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>{@code POST /estudiantes} — se necesita convocatoria actual para crear la participación</li>
 *   <li>{@code PUT /estudiantes/{id}} — se necesita convocatoria actual para actualizar la participación</li>
 *   <li>{@code DELETE /estudiantes/{id}} — se necesita convocatoria actual para eliminar la participación</li>
 *   <li>{@code GET /estudiantes} — cuando idConvocatoria no se especifica y no hay convocatoria activa</li>
 *   <li>{@code GET /estudiantes/{id}} — cuando idConvocatoria no se especifica y no hay convocatoria activa</li>
 * </ul>
 */
public class ConvocatoriaActualNoEncontradaException extends RuntimeException {
    public ConvocatoriaActualNoEncontradaException() {
        super("No hay ninguna convocatoria activa en este momento");
    }
}
