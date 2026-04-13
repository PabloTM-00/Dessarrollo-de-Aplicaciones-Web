package es.uma.informatica.daw.practicajpa.entidades;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Set;

@Getter
@Setter
@Entity
public class Federacion {

    @Id
    private Long id;

    private String nombre;
    @OneToMany(mappedBy = "federacion")
    private Set<Pais> paises;

}