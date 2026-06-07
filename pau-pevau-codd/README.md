[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/-TiKKTTS)

Para generar el DDL:
```bash
mvn test
```

Se deberían haber generado dos archivos: `target/borrar_esquema.sql` y `target/crear_esquema.sql`.

```
pau-pevau-codd
├── docs
│  ├── fix_entities-jpa-mappings-report.md
│  └── plan.md
├── pevau
│  ├── .mvn
│  │  └── wrapper
│  │    └── maven-wrapper.properties
│  ├── src
│  │  ├── main
│  │  │  ├── java
│  │  │  │  └── es
│  │  │  │    └── uma
│  │  │  │      └── informatica
│  │  │  │        └── daw
│  │  │  │          └── pevau
│  │  │  │            ├── entities
│  │  │  │            │  ├── EstudianteEntity.java
│  │  │  │            │  └── InstitutoEntity.java
│  │  │  │            ├── repositories
│  │  │  │            │  ├── EstudianteRepository.java
│  │  │  │            │  └── InstitutoRepository.java
│  │  │  │            └── PevauApplication.java
│  │  │  └── resources
│  │  │    └── application.properties
│  │  └── test
│  │    └── java
│  │      └── es
│  │        └── uma
│  │          └── informatica
│  │            └── daw
│  │              └── pevau
│  │                └── PevauApplicationTests.java
│  ├── mvnw
│  ├── mvnw.cmd
│  └── pom.xml
└── README.md
```
