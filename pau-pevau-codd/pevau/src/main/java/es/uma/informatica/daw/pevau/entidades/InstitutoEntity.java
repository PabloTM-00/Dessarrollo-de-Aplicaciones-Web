package es.uma.informatica.daw.pevau.entidades;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Entity
@Table(name = "institutos")
@Data
@Accessors(chain = true)
// Excluimos "estudiantes" para evitar referencia circular con EstudianteEntity
// que causaría un hashCode infinito y LazyInitializationException de Hibernate
@EqualsAndHashCode(exclude = "estudiantes")
public class InstitutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    private String direccion1;
    private String direccion2;
    private String localidad;

    @Column(name = "codigo_postal")
    private Integer codigoPostal;

    private String pais;

    @OneToMany(mappedBy = "instituto", fetch = FetchType.EAGER)
    private Set<EstudianteEntity> estudiantes = new HashSet<>();
}