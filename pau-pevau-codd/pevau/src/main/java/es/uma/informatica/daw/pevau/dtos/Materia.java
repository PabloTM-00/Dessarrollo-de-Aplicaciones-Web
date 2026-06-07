package es.uma.informatica.daw.pevau.dtos;

/**
 * DTO para representar la información de una materia, obtenida de un servicio externo.
 * Utilizado como objeto anidado dentro de Estudiante.
 */
public record Materia(
        Long id,
        String nombre,
        boolean eliminada
) {
}