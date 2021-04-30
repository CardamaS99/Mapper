CREATE TABLE Person (
    username varchar(45) primary key,
    passwd varchar(256) not null,
    firstName varchar(45),
    idJob int,

    FOREIGN KEY (idJob) REFERENCES Job(id);
);

CREATE TABLE Job (
    id int primary key,
    name text not null
);