package es.uma.informatica.daw.practicapruebas.excepciones;

public class FueraHorarioLaboralException extends RuntimeException {
    public FueraHorarioLaboralException() {
        super("La cita no puede ser programada fuera del horario laboral");
    }
}
