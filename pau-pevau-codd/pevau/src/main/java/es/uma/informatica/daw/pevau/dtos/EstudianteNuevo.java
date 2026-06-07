package es.uma.informatica.daw.pevau.dtos;

import java.util.Set;

/**
 * DTO para la creación o actualización de un estudiante.
 * Contiene solo los identificadores de las relaciones con Instituto y Materias.
 */
public record EstudianteNuevo(
        NombreCompleto nombreCompleto,
        String dni,
        String telefono,
        String email,
        Set<Long> materiasMatriculadas, // Solo identificadores de materias
        Long idInstituto, // Identificador del instituto
        Long idSede, // Identificador de la sede
        boolean noEliminar
) {
}