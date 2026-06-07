package es.uma.informatica.daw.pevau.dtos;

/**
 * DTO auxiliar para detallar un problema durante la importación de estudiantes.
 * Contiene el estudiante que causó el problema y una descripción del error.
 */
public record ProblemaImportacion(
        Estudiante estudiante, // El estudiante que causó el problema
        String problemaImportacion // Descripción del error
) {
}