package es.uma.informatica.daw.pevau.dtos;

import java.util.List;

/**
 * DTO para la respuesta de la importación masiva de estudiantes.
 * Contiene listas de estudiantes importados exitosamente y de aquellos con errores.
 */
public record ImportacionEstudiantes(
        List<Estudiante> importados, // Estudiantes creados exitosamente (DTO completos)
        List<ProblemaImportacion> noImportados // Registros que no pudieron importarse
) {
}