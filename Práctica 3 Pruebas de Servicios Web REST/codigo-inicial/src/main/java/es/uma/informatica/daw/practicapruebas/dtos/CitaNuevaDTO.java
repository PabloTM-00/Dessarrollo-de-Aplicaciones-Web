package es.uma.informatica.daw.practicapruebas.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CitaNuevaDTO {
    private String cliente;
    private LocalDateTime inicio;
    private Integer duracion; //duración en minutos
}
