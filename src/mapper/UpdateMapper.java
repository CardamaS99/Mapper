package mapper;




import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Database conection and update wrapper. Automatically maps updated data
 *
 * @param <T> Mapped class type. Used to check asigments while mapping
 * @author luastan
 * @author CardamaS99
 * @author danimf99
 * @author alvrogd
 * @author OswaldOswin1
 * @author Marcos-marpin
 */
public class UpdateMapper<T> extends Mapper<T> {
    private Class<T> clase;
    private List<T> elementsUpdate;
    private ArrayList<String> columnNames;
    private HashMap<String, Field> attributes;

    public UpdateMapper(Connection connection) {
        super(connection);
        this.elementsUpdate = new ArrayList<>();
        this.columnNames = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    /**
     * Defines the class to be used ehen updating the items.
     *
     * @param clase Class type of the elements to be updated
     * @return UpdateMapper instance
     */
    @Override
    public UpdateMapper<T> defineClass(Class<? extends T> clase) {
        super.defineClass(clase);
        return this;
    }

    /**
     * Adds an object to de update pool. It does not get updated on the
     * database until method {@link UpdateMapper#update(boolean)} gets called.
     *
     * @param object Object to be updated
     * @return Current UpdateMapper instance
     */
    public UpdateMapper<T> add(T object) {
        this.elementsUpdate.add(object);
        return this;
    }

    /**
     * Adds multiple objects to the update pool. Check {@link UpdateMapper#add(Object)}
     * in order to get extra info about the update pool behaviour.
     *
     * @param objects Objects to be updated in the database
     * @return UpdateMapper instance
     */
    public UpdateMapper<T> addAll(T... objects) {
        this.elementsUpdate.addAll(Arrays.asList(objects));
        return this;
    }

    @Override
    public UpdateMapper<T> defineParameters(Object... parametros) throws Exception {
        super.defineParameters(parametros);
        return this;
    }

    /**
     * Stores the given isolation level to apply it when executing the constructed transaction
     *
     * @param isolationLevel desired transaction isolation level
     * @return update mapper which is being built
     */
    @Override
    public UpdateMapper<T> setIsolationLevel(int isolationLevel) throws Exception {

        return((UpdateMapper<T>)super.setIsolationLevel(isolationLevel));
    }

    /**
     * Updates the objects from the update pool on the database
     *
     * @param allowNullValues On true allows null values to be inserted into the database
     */
    public void update(boolean allowNullValues) throws Exception {
        PreparedStatement statement;
        String columnName;
        StringBuilder updateBuilder = new StringBuilder("UPDATE ").append(mappedClass.getAnnotation(MapperTable.class)
                .nombre()).append(" SET ");

        // Configures the connection to the database
        configureConnection();

        /* SET clause building */

        // Loops over the elements to be updated
        for (T objectUpdate : this.elementsUpdate) {

            // Loops over the object's class fields making them accessible
            for (Field field : mappedClass.getDeclaredFields()) {
                field.setAccessible(true);

                // Se comprueba que el atributo tiene @MapperColumn y que no es null
                try {
                    // Only annotated and non null which are not primary keys fields get updated into the database
                    if (field.isAnnotationPresent(MapperColumn.class) &&
                            (allowNullValues || field.get(objectUpdate) != null) &&
                            !field.getAnnotation(MapperColumn.class).pkey()) {
                        // Colum name for the current Field. If it's not annotated, field name gets used
                        columnName = extractColumnName(field);

                        // Value assignment
                        updateBuilder.append(columnName).append(" = ?,");

                        // Column names and fields get stored for later use
                        this.columnNames.add(columnName);
                        this.attributes.put(columnName, field);
                    }
                } catch (IllegalAccessException e) {
                    throw new Exception(e.getMessage());
                }
            }

            // Crops the update builder to get rid of the extra ","
            updateBuilder.deleteCharAt(updateBuilder.length() - 1);

            /* WHERE building */

            updateBuilder.append(" WHERE ");
            for (Field field : mappedClass.getDeclaredFields()) {
                // Allows reflection on the Field
                field.setAccessible(true);

                // Performs the actions on Mappeable fields which also happen to be Primary Keys
                if (field.isAnnotationPresent(MapperColumn.class) && field.getAnnotation(MapperColumn.class).pkey()) {
                    // Colum name for the current Field. If it's not annotated, field name gets used
                    columnName = field.getAnnotation(MapperColumn.class).columna();
                    columnName = columnName.equals("") ? field.getName() : columnName;

                    // Value check
                    updateBuilder.append(columnName).append(" = ?,");

                    // Column names and fields get stored for later use
                    this.columnNames.add(columnName);
                    this.attributes.put(columnName, field);
                }
            }

            // Crops the update builder to clean the "," once again
            updateBuilder.deleteCharAt(updateBuilder.length() - 1);

            try {
                statement = connection.prepareStatement(updateBuilder.toString());

                // Loops over all the columns to be inserted while obtaining the value from reflection
                for (int i = 0; i < this.columnNames.size(); i++) {
                    statement.setObject(i + 1, this.attributes.get(this.columnNames.get(i)).get(objectUpdate));
                }

                // Update gets executed
                statement.executeUpdate();
                statement.close();
            } catch (SQLException | IllegalAccessException e) {
                throw new Exception(e.getMessage());
            }

            // Temporary data gets cleared
            this.columnNames.clear();
            this.attributes.clear();
        }
    }


    /**
     * Performs an update without allowing NULL values into the database.
     * Check {@link UpdateMapper#update(boolean)} for more info
     */
    public void update() throws Exception {
        this.update(false);
    }

}
