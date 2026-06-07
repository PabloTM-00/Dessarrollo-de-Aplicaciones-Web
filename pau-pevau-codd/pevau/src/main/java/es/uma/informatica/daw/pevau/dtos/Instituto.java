package es.uma.informatica.daw.pevau.dtos;

/**
 * DTO para representar la información de un instituto.
 * Utilizado para peticiones y respuestas relacionadas con la gestión de institutos.
 */
public record Instituto(
        Long id,
        String nombre,
        String direccion1,
        String direccion2,
        String localidad,
        Integer codigoPostal,
        String pais
) {
}