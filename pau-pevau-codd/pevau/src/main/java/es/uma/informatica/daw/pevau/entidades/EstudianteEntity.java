package es.uma.informatica.daw.pevau.entidades;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.EqualsAndHashCode;


import java.util.HashSet;
import java.util.Set;

/**
 * Representa la participación de un estudiante (persona) en una convocatoria concreta.
 * Si una persona participa en N convocatorias, existirán N instancias de esta entidad,
 * una por convocatoria.
 */
@Entity
@Table(
    name = "estudiantes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"dni", "id_convocatoria"})
)
@Data
@Accessors(chain = true)
// Excluimos "instituto" para evitar referencia circular con InstitutoEntity
// que causaría un hashCode infinito y LazyInitializationException de Hibernate
@EqualsAndHashCode(exclude = "instituto")
public class EstudianteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apellido1", nullable = false)
    private String apellido1;

    @Column(name = "apellido2", nullable = false)
    private String apellido2;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "dni", nullable = false)
    private String dni;

    @Column(name = "telefono", nullable = false)
    private String telefono;

    @Column(name = "email", nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "instituto_id", nullable = false)
    private InstitutoEntity instituto;


    @Column(name = "id_convocatoria", nullable = false)
    private Long idConvocatoria;

    @Column(name = "id_sede", nullable = false)
    private Long idSede;

    @Column(name = "no_eliminar", nullable = false)
    private boolean noEliminar;

    @ElementCollection
    @CollectionTable(name = "estudiante_materias", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "materia_id")
    private Set<Long> materiasMatriculadas = new HashSet<>();
}
