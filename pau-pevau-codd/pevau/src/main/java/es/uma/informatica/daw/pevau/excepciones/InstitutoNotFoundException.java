package es.uma.informatica.daw.pevau.excepciones;

public class InstitutoNotFoundException extends RuntimeException {
    public InstitutoNotFoundException(String id) {
        super("No existe ningún instituto con ID: " + id);
    }
}