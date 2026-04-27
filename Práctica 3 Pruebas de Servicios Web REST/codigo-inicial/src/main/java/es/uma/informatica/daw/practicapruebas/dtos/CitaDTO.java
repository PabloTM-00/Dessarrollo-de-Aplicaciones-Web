package es.uma.informatica.daw.practicapruebas.dtos;

import es.uma.informatica.daw.practicapruebas.entidades.EstadoCita;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CitaDTO extends CitaNuevaDTO{
    private Long id;
    private EstadoCita estado;
    private LocalDateTime fin;
}
