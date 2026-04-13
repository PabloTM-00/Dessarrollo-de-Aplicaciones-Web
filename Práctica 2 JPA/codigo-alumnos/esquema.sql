
    create table estado (
        federacion_id bigint,
        id bigint not null,
        nb_habitantes bigint,
        nombre_pais varchar(255),
        primary key (id)
    );

    create table estado_colindantes (
        colindantes_id bigint not null,
        pais_id bigint not null,
        primary key (colindantes_id, pais_id)
    );

    create table federacion (
        id bigint not null,
        nombre varchar(255),
        primary key (id)
    );

    alter table if exists estado 
       add constraint FK12sqa2pu080r35dtj0f1tcd4m 
       foreign key (federacion_id) 
       references federacion;

    alter table if exists estado_colindantes 
       add constraint FKm6xgwhl3s4hbr27br7uec90v0 
       foreign key (colindantes_id) 
       references estado;

    alter table if exists estado_colindantes 
       add constraint FKd73l69e35g5jn1ig0q1fmrvnw 
       foreign key (pais_id) 
       references estado;
