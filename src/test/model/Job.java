package test.model;

import mapper.MapperColumn;
import mapper.MapperTable;

@MapperTable(name = "Job")
public class Job {

    @MapperColumn(pkey = true)
    private int id;

    @MapperColumn
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
