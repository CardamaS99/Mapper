package mapper;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Database conection and data retrieving wrapper. Automatically maps retreved
 * data
 *
 * @param <T> Mapped class type. Used to check asigments when returning query
 *            results.
 * @author luastan
 * @author CardamaS99
 * @author danimf99
 * @author alvrogd
 * @author OswaldOswin1
 * @author Marcos-marpin
 */
public class QueryMapper<T> extends Mapper<T> {

    /**
     * @param conexion Database conection object
     */
    public QueryMapper(Connection conexion) {
        super(conexion);
    }

    /**
     * Defines the sentence to be queried to the database
     *
     * @param query String representing the query
     * @return Returns the Mapper instance
     */
    public QueryMapper<T> createQuery(String query) throws Exception {
        try {
            statement = connection.prepareStatement(query);
        } catch (SQLException ex) {
            throw new Exception(ex.getMessage());
        }
        return this;
    }

    /**
     * Get all information about the object.
     *
     * @param object object with the primary keys
     * @return object with all information from the database.
     * @throws Exception
     */
    public T get(T object) throws Exception {
        StringBuilder builder = new StringBuilder("SELECT * FROM ");
        List<Object> attributes = new ArrayList<>();

        builder.append(mappedClass.getAnnotation(MapperTable.class).name()).append(" WHERE ");

        for (Field field : mappedClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(MapperColumn.class)) {
                MapperColumn column = field.getAnnotation(MapperColumn.class);

                if (column.pkey()) {
                    builder.append(extractColumnName(field)).append("=? AND ");

                    // Adds primary keys to the attributes list.
                    attributes.add(field.get(object));
                }
            }
        }

        builder.delete(builder.length() - 5, builder.length());

        System.out.println(builder.toString());
        System.out.println(attributes);

        createQuery(builder.toString());
        defineParametersList(attributes);

        return findFirst();
    }

    /**
     * Returns a list with the query results propperly mapped to the Class
     * defined at {@link QueryMapper#defineClass(Class)}
     *
     * @param useForeignKeys When true will attempt to find foreign keys when
     *                       the class representing the atributes has the
     *                       {@link MapperTable} annotation present
     * @return Mapped objects from the query
     */
    public List<T> list(boolean useForeignKeys) throws Exception {
        ArrayList<T> resultado = new ArrayList<>();
        String nombreColumna;
        Matcher matcher;
        HashMap<String, Object> fkValues;
        HashSet<String> columnas = new HashSet<>();
        T elemento;
        Class<?> foreignClass;
        Boolean notPresent;

        // Configures the connection to the database
        configureConnection();

        try {
            statement.execute();
            ResultSet set = statement.getResultSet();

            // Metadata parsing
            if (set != null) {
                for (int i = 1; i <= set.getMetaData().getColumnCount(); i++) {
                    columnas.add(set.getMetaData().getColumnName(i));
                }

                while (set.next()) {
                    // Extracts required empty constructor and Fields to map the resutls
                    elemento = mappedClass.getConstructor(new Class[]{}).newInstance();
                    for (Field field : mappedClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.isAnnotationPresent(MapperColumn.class)) {
                            nombreColumna = extractColumnName(field);
                            if (columnas.contains(nombreColumna) || field.getAnnotation(MapperColumn.class).targetClass() != Object.class) {
                                foreignClass = field.getAnnotation(MapperColumn.class).targetClass();
                                // Checks if the Field class has the MapperTable anotation. This means that it's a
                                // foreign key and special actions are required
                                if (foreignClass != Object.class &&
                                        foreignClass.isAnnotationPresent(MapperTable.class)) {
                                    if (useForeignKeys) {
                                        // FKEYS
                                        if (field.getAnnotation(MapperColumn.class).fKeys().equals("")) {
                                            if (columnas.contains(nombreColumna)) {
                                                field.set(elemento, getFK(foreignClass, set.getObject(nombreColumna)));
                                            }
                                        } else {
                                            notPresent = false;
                                            fkValues = new HashMap<>();
                                            matcher = regexFKeys.matcher(field.getAnnotation(MapperColumn.class).fKeys());
                                            while (matcher.find()) {
                                                if (columnas.contains(matcher.group(1))) {
                                                    fkValues.put(matcher.group(2), set.getObject(matcher.group(1)));
                                                } else {
                                                    notPresent = true;
                                                }
                                            }
                                            if (!notPresent) {
                                                field.set(elemento, getFK(foreignClass, fkValues));
                                            }

                                        }

                                    }

                                } else {
                                    field.set(elemento, set.getObject(nombreColumna));
                                }
                            }
                        }
                    }
                    resultado.add(elemento);
                }
            }
            statement.close();

            // Exception handling
        } catch (SQLException | IllegalAccessException | InvocationTargetException | InstantiationException |
                NoSuchMethodException e) {
            throw new Exception(e.getMessage());
        }
        return resultado;
    }

    /**
     * Does the same as {@link QueryMapper#list(boolean)} with the foreign keys
     * boolean as true
     *
     * @return Mapped objects list
     */
    public List<T> list() throws Exception {
        return this.list(true);
    }


    /**
     * Defines the class to be used in the mapping process.
     *
     * @param mappedClass Class used to map the query results
     * @return The QueryMapper instance
     */
    @Override
    public QueryMapper<T> defineClass(Class<? extends T> mappedClass) {
        super.defineClass(mappedClass);
        return this;
    }

    /**
     * Defines the parameters used when executing the query. This parameters
     * are defined with ? in the {@link QueryMapper#createQuery(String)} String
     *
     * @param parametros Parameter list to be inserted into the {@link java.sql.PreparedStatement}
     *                   used to query the database
     * @return The QueryMapper instance
     */
    public QueryMapper<T> defineParameters(Object... parametros) throws Exception {
        super.defineParameters(parametros);
        return this;
    }

    /**
     * Defines the parameters used when executing the query. This parameters
     * are defined with ? in the {@link QueryMapper#createQuery(String)} String
     *
     * @param parametros Parameter list to be inserted into the {@link java.sql.PreparedStatement}
     *                   used to query the database
     * @return The QueryMapper instance
     */
    public QueryMapper<T> defineParametersList(List<Object> parametros) throws Exception {
        super.defineParametersList(parametros);
        return this;
    }

    /**
     * Stores the given isolation level to apply it when executing the constructed transaction
     *
     * @param isolationLevel desired transaction isolation level
     * @return query mapper which is being built
     */
    @Override
    public QueryMapper<T> setIsolationLevel(int isolationLevel) throws Exception {

        return ((QueryMapper<T>) super.setIsolationLevel(isolationLevel));
    }

    /* Closing methods */

    /**
     * When the Cass to be used wasn't defined with {@link QueryMapper#defineClass(Class)}
     * this method resturns the query results mapped into a list containing
     * {@link Map} instances with the column names used as keys in the Map, and
     * the values from the result tuples
     *
     * @return Map list with the query Results
     */
    public List<Map<String, Object>> mapList() throws Exception {
        List<Map<String, Object>> resultadosMapeados = new ArrayList<>();
        Map<String, Object> element;
        ArrayList<String> columnas = new ArrayList<>();
        ResultSet set;

        // Configures the connection to the database
        configureConnection();

        try {
            statement.execute();
            set = statement.getResultSet();
            for (int i = 1; i <= set.getMetaData().getColumnCount(); i++) {
                columnas.add(set.getMetaData().getColumnName(i));
            }
            while (set.next()) {
                element = new HashMap<>();
                for (String columna : columnas) {
                    element.put(columna, set.getObject(columna));
                }
                resultadosMapeados.add(element);
            }
        } catch (SQLException e) {
            throw new Exception(e.getMessage());
        }
        return resultadosMapeados;
    }

    /**
     * From the results, returns the first one. Usefull when querying a single
     * item. It performs the whole Mapping process which can be seen as
     * ineficient. For increased efficiency add limit(1) to de query.
     *
     * @param useForeignkeys Same atribute as in {@link QueryMapper#list(boolean)}
     * @return First element from the results
     */
    public T findFirst(boolean useForeignkeys) throws Exception {
        List<T> result = list(useForeignkeys);

        if (result == null || result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }


    /**
     * Check {@link QueryMapper#findFirst(boolean)}. Does the same as findFirst(true)
     *
     * @return First element from the results
     */
    public T findFirst() throws Exception {
        return findFirst(true);
    }
}
