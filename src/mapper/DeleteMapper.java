package mapper;



import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;


/**
 * Database deletions wrapper
 *
 * @param <T> Mapped class type. Used to check asigments on the mapped class
 * @author luastan
 * @author CardamaS99
 * @author danimf99
 * @author alvrogd
 * @author OswaldOswin1
 * @author Marcos-marpin
 */
public class DeleteMapper<T> extends Mapper<T> {
    private List<T> elementsDelete;
    private String deleteUpdate;
    private ArrayList<String> columnsName;
    private HashMap<String, Field> attributes;

    /**
     * @param connection Database conection
     */
    public DeleteMapper(Connection connection) {
        super(connection);
        elementsDelete = new ArrayList<>();
        deleteUpdate = "";
        columnsName = new ArrayList<>();
        attributes = new HashMap<>();
    }

    /**
     * Defines the class representing the elements to be deleted
     *
     * @param clase Class to be mapped
     * @return Delete Mapper instance
     */
    @Override
    public DeleteMapper<T> defineClass(Class<? extends T> clase) {
        super.defineClass(clase);
        return this;
    }

    /**
     * Adds an object to be deleted. It does not get deleted until {@link DeleteMapper#delete()}
     * method gets executed
     *
     * @param object Object to be deleted
     * @return DeleteMapper instance
     */
    public DeleteMapper<T> add(T object) {
        this.elementsDelete.add(object);
        return this;
    }

    /**
     * Adds multiple obkects to the deletion pool. Check {@link DeleteMapper#add(Object)}
     *
     * @param objects Objects to be deleted
     * @return DeleteMapper instance
     */
    public DeleteMapper<T> addAll(T... objects) {
        this.elementsDelete.addAll(Arrays.asList(objects));
        return this;
    }

    /**
     * Stores the given isolation level to apply it when executing the constructed transaction
     *
     * @param isolationLevel desired transaction isolation level
     * @return deletion mapper which is being built
     */
    @Override
    public DeleteMapper<T> setIsolationLevel(int isolationLevel) throws Exception {

        return((DeleteMapper<T>)super.setIsolationLevel(isolationLevel));
    }

    /**
     * Extracts the primary keys and genterates the corresponding SQL code
     */
    private void prepareDelete() throws Exception {
        String columnName;

        // SQL code base. Needed info gets extracted from the mapped class
        StringBuilder deleteBuilder = new StringBuilder("DELETE FROM ")
                .append(mappedClass.getAnnotation(MapperTable.class).name()).append(" WHERE ");

        // Loops over all the fields from the Mapped class
        for (Field field : mappedClass.getDeclaredFields()) {
            // Allows access from reflection
            field.setAccessible(true);
            // Performs the mapping only on the annotated classes
            if (field.isAnnotationPresent(MapperColumn.class) && field.getAnnotation(MapperColumn.class).pkey()) {
                // Column name extraction
                // On empty / default column name specification uses the field name
                columnName = extractColumnName(field);

                // Adds check conditions on every primary key
                deleteBuilder.append(columnName).append(" = ? and ");

                // Column names and Class fields get stored to be used later on the deletion
                this.columnsName.add(columnName);
                this.attributes.put(columnName, field);
            }
        }

        // Crops the deleteBuilder in order to get rid of the residual "and" added after each WHERE condition
        deleteBuilder.delete(deleteBuilder.length() - 4, deleteBuilder.length());

        // Stores the SQL code to be executed
        deleteUpdate = deleteBuilder.toString();

        try {
            // TODO: This code seems redundant to me. It gets lost on the delete() method
            this.statement = super.connection.prepareStatement(deleteUpdate);
        } catch (SQLException ex) {
            throw new Exception(ex.getMessage());
        }
    }

    /**
     * Deletes all the objects on the deletion pool
     */
    public void delete() throws Exception {
        prepareDelete();  // Builds the statement

        // Configures the connection to the database
        configureConnection();

        try {
            // Loops over the deletion pool deleting each object
            for (T object : this.elementsDelete) {
                // Statement gets created
                this.statement = connection.prepareStatement(this.deleteUpdate);

                // The atomic PKs
                Map<String, Object> atomicPKs = getAtomicPK(object);
                // Inserts all the primary key atributes previously extracted into the statement
                for (int i = 0; i < this.columnsName.size(); i++) {
                    Field field = this.attributes.get(this.columnsName.get(i));
                    Object obj = field.get(object);

                    if(!isAtomicClass(obj.getClass())){
                        obj = atomicPKs.get(this.columnsName.get(i));
                    }

                    statement.setObject(i + 1, obj);
                }

                // Deletion gets performed
                this.statement.executeUpdate();
            }
        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}
