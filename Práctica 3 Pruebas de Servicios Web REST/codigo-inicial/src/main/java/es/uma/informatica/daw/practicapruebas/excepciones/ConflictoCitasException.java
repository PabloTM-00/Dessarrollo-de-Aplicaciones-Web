package es.uma.informatica.daw.practicapruebas.excepciones;

public class ConflictoCitasException extends RuntimeException {
        public ConflictoCitasException() {
            super("Hay un conflicto con otra cita existente");
        }
}
