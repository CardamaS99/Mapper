package test.model;

import mapper.MapperColumn;
import mapper.MapperTable;

@MapperTable(name = "Person")
public class User {

    @MapperColumn(pkey = true)
    private String username;

    @MapperColumn(column="firstName")
    private String name;

    @MapperColumn(column="passwd", notNull = true)
    private String password;

    @MapperColumn(fKeys = "idJob:id", targetClass = Job.class)
    private Job job;

    public User() {

    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Job getJob() {
        return job;
    }
}
