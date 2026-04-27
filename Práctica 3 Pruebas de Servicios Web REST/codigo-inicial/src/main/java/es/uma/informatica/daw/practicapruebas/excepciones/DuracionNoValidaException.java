package es.uma.informatica.daw.practicapruebas.excepciones;

public class DuracionNoValidaException extends RuntimeException {
    public DuracionNoValidaException(String mensaje) {
        super(mensaje);
    }
}
