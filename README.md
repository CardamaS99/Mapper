# Mapper

## ¿What is it?

_Mapper_ is a library that allow the __object-relational mapping__ from Relational Databases to Java objects.

## ¿Why was it developed?

This library was developed as part of an assigment in the subject __Databases II__ from the __Computer Engineering Degree__ in the __University of Santiago de Compostela__. Specifically, it consisted in the development of a social network which required the usage of SQL databases.

## ¿How does it work?

### ¿How to define the mapping of a Java class?
The library provides the user with an interface which relies on the usage of the _annotations_ available in Java. These can be classified in two categories:

1. __MapperTable:__ _class level_ annotation that points the library which is the database's __table__ from where the data will be mapped.
2. __MapperColumn:__ _attribute level_ annotation that points the library which is the database's __column__ from where the data will be obtained.

Via these annotations, the library is able to __parse basic SQL tables__ along with their properties, such as primary keys, or default values. Following up, an example shows a Java class _Alumno_ which maps the _Alumnos_ table.

```
@MapperTable(nombre="alumnos")
public class Alumno {

    @MapperColumn(columna="name")
    private String nombre;
    
    @MapperColumn(pkey=true)
    private String dni;
    
    @MapperColumn
    private Date birthday;
    
    @MapperColumn(fkey="profesor:id")
    private Profesor profesor;
}
```

### ¿How to perform the mapping of Java class?

To map an already defined Java class using __annotations__, the user will need to rely on the available _Mapper_ classes in the library. Within these, a parent class is divided into several subclasses, each one specializing in an specific kind of tasks:

1. __InsertionMapper:__ performs the insertion of an object in the database.
2. __QueryMapper:__ performs queries over the database, allowing the recovery of every present information in the shape of a mapped class' instance.
3. __DeleteMapper:__ performs the deletion of the specified object in the database.
4. __UpdateMapper:__ performs the update of the specified object's data in the database.

## Advantages of using the Mapper

Some of the benefits that the Mapper can provide to a software project are:

1. __Disengage the connection__ with the database through the usage of the mapper as an intermediary in the sharing of data.
2. __Increase of the security__, as the mapper with take care of preventing common security flaws in software such as SQL injection.
3. __Ease of executing SQL sentences__, due to the abstraction that the mapper provides over the connection with the database.
4. The possibility of __using non-atomic classes as foreign keys__ (namely, using a Java class as a foreign key instead of a single attribute); in addition, the mapper is able to extract from the database all the information about the referenced class as an object, not only its __primary key__, but also its other data regardless of its type, providing the user with the appropiate abstraction.
