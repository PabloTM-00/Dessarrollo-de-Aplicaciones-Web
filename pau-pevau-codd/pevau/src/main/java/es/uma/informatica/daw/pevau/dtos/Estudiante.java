package es.uma.informatica.daw.pevau.dtos;

import java.util.Set;

/**
 * DTO para representar la información completa de un estudiante en las respuestas.
 * Incluye objetos anidados para Instituto y Materias.
 */
public record Estudiante(
        Long id,
        NombreCompleto nombreCompleto,
        String dni,
        String telefono,
        String email,
        Set<Materia> materiasMatriculadas, // Objetos Materia completos
        Long idSede, // Identificador de la sede
        Instituto instituto, // Objeto Instituto completo
        boolean noEliminar
) {
}