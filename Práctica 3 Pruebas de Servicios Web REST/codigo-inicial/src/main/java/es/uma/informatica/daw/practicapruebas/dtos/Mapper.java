package es.uma.informatica.daw.practicapruebas.dtos;

import es.uma.informatica.daw.practicapruebas.entidades.Cita;

public class Mapper {

    public static CitaDTO toCitaDTO(Cita cita) {
        if (cita == null) {
            return null;
        }
        CitaDTO dto = new CitaDTO();
        dto.setId(cita.getId());
        dto.setDuracion(cita.getDuracion());
        dto.setEstado(cita.getEstado());
        dto.setCliente(cita.getCliente());
        dto.setFin(cita.getFin());
        return dto;
    }

    public static Cita toCita(CitaNuevaDTO dto) {
        if (dto == null) {
            return null;
        }
        Cita cita = new Cita();
        cita.setInicio(dto.getInicio());
        cita.setDuracion(dto.getDuracion());
        cita.setCliente(dto.getCliente());
        return cita;
    }
}
