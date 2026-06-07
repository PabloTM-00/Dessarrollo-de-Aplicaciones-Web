package es.uma.informatica.daw.pevau.excepciones;

/**
 * Excepción lanzada cuando se intenta crear o actualizar un estudiante
 * con un DNI que ya existe en la base de datos.
 * <p>
 * Esta excepción debe traducirse a HTTP 409 Conflict en el controlador,
 * indicando al cliente que el DNI ya está registrado.
 * </p>
 * 
 * <p>Casos de uso:</p>
 * <ul>
 *   <li>{@code POST /estudiantes} con DNI ya existente</li>
 *   <li>{@code PUT /estudiantes/{id}} cambiando el DNI a uno ya existente</li>
 *   <li>Importación CSV con DNI duplicado</li>
 * </ul>
 */

public class DniDuplicadoException extends RuntimeException {
    public DniDuplicadoException(String dni) {
        super("Ya existe un estudiante con DNI: " + dni);
    }
}