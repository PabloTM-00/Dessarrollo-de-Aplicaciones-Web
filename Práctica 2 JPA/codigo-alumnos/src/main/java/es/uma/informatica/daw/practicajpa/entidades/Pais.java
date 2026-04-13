package es.uma.informatica.daw.practicajpa.entidades;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "estado")
public class Pais {

    @Id
    private Long id;

    @Column(name = "nombre_pais")
    private String nombre;

    @Column(name = "nb_habitantes")
    private Long habitantes;

    @ManyToOne
    private Federacion federacion;

    @ManyToMany
    private Set<Pais> colindantes;
}