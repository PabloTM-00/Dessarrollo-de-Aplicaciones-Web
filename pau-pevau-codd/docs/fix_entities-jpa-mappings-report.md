# Reporte de Correcciones Técnicas: Rama `fix/entities-jpa-mappings`

Este documento detalla los cambios realizados en la entidad `EstudianteEntity` para corregir errores de persistencia y asegurar el cumplimiento de las reglas de negocio.

## 1. Refactorización de Nombres
- **Cambio:** Se renombró la clase `Estudiante` a `EstudianteEntity` e `Instituto` a `InstitutoEntity`.
- **Razón:** Seguir las convenciones del plan de implementación y diferenciar claramente las entidades de JPA de los objetos de transferencia de datos (DTO).

## 2. Corrección de la Relación Estudiante con Instituto
- **Cambio:** Se cambió la anotación `@OneToOne` por `@ManyToOne`.
- **Razón:** Un instituto puede tener múltiples estudiantes matriculados. La anotación `@OneToOne` restringía la base de datos de manera errónea, permitiendo solo un estudiante por cada instituto.

## 3. Persistencia de Colecciones: `@ElementCollection` (Detalle Crítico) en el campo `materiasMatriculadas` de Estudiante

### El Problema
En la versión anterior, el campo `materiasMatriculadas` estaba declarado simplemente como `List<Long>`. En JPA, las bases de datos relacionales no pueden almacenar una lista de elementos directamente en una sola celda de la tabla de estudiantes. Sin una configuración específica, esto provoca un error al arrancar la aplicación o ignora el campo por completo.

### La Solución
Se implementó el uso de `@ElementCollection` junto con `@CollectionTable`.

```java
@ElementCollection
@CollectionTable(name = "estudiante_materias", joinColumns = @JoinColumn(name = "estudiante_id"))
@Column(name = "materia_id")
private Set<Long> materiasMatriculadas;
```

### ¿Por qué se hizo así?
1. **Tabla Auxiliar:** `@CollectionTable` crea automáticamente una tabla secundaria llamada `estudiante_materias`. Esto permite que la base de datos guarde cada ID de materia en una fila separada vinculada al ID del estudiante.
2. **Uso de `Set` en lugar de `List`:** Se cambió el tipo a `Set` para garantizar que un estudiante no pueda estar matriculado dos veces en la misma materia (integridad de datos).
3. **Desacoplamiento:** Como las materias residen en otro microservicio, no usamos una relación `@ManyToMany`. Almacenar solo los IDs mediante `@ElementCollection` es la forma correcta de referenciar datos externos manteniendo la simplicidad.

## 4. Integridad del DNI
- **Cambio:** Se añadió `unique = true` en la anotación `@Column(name = "dni")`.
- **Razón:** El DNI es un identificador único natural. Esta restricción a nivel de base de datos evita que se introduzcan registros duplicados por error en la lógica de negocio o fallos en las peticiones API.

---
*Nota: Estas correcciones son fundamentales para que el microservicio sea funcional y escalable según los requisitos del proyecto.*
