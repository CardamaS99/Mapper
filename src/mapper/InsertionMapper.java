package mapper;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


/**
 * Database conection and data insertion wrapper. Performs insertions from
 * given objects without actually specifying SQL sentences.
 *
 * @param <E> Mapped class type. Used to check asigments when returning query
 *            results.
 * @author luastan
 * @author CardamaS99
 * @author danimf99
 * @author alvrogd
 * @author OswaldOswin1
 * @author Marcos-marpin
 */
public class InsertionMapper<E> extends Mapper<E> {
    private List<E> insertions;
    private String query;
    private ArrayList<String> columnas;
    private HashMap<String, Field> atributos;


    public InsertionMapper(Connection conexion) {
        super(conexion);
        insertions = new ArrayList<>();
        query = "";
        columnas = new ArrayList<>();
        atributos = new HashMap<>();
    }


    /**
     * Defines the Class of the insertions
     *
     * @param mappedClass Class corresponding the elements to be inserted
     * @return The IsertionMapper instance
     */
    @Override
    public InsertionMapper<E> defineClass(Class<? extends E> mappedClass) {
        super.defineClass(mappedClass);
        return this;
    }

    /**
     * Adds an object to be inserted
     *
     * @param objeto Object to be inserted
     * @return The current InsertionMapper instance
     */
    public InsertionMapper<E> add(E objeto) {
        insertions.add(objeto);
        return this;
    }


    /**
     * Adds multiple objects to the insertion tool
     *
     * @param objects Objects that are goint to be inserted
     * @return El propio insertionMapper
     */
    public InsertionMapper<E> addAll(E... objects) {
        insertions.addAll(Arrays.asList(objects));
        return this;
    }

    /**
     * Stores the given isolation level to apply it when executing the constructed transaction
     *
     * @param isolationLevel desired transaction isolation level
     * @return insertion mapper which is being built
     */
    @Override
    public InsertionMapper<E> setIsolationLevel(int isolationLevel) throws Exception {

        return((InsertionMapper<E>)super.setIsolationLevel(isolationLevel));
    }

    /**
     * Extracts the atributes and fields to be inserted into the database and
     * generates the corresponding SQL sentence base for the insertions
     */
    private void prepareQuery() {
        String nombreColumna;
        StringBuilder queryBuilder = new StringBuilder("INSERT INTO ")
                .append(mappedClass.getAnnotation(MapperTable.class).name()).append("(");

        // Fetches all the fields to be mapped
        for (Field field : mappedClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(MapperColumn.class) && !field.getAnnotation(MapperColumn.class).hasDefault()) {
                nombreColumna = extractColumnName(field);
                queryBuilder.append(nombreColumna).append(",");
                this.columnas.add(nombreColumna);
                this.atributos.put(nombreColumna, field);
            }
        }
        queryBuilder.deleteCharAt(queryBuilder.length() - 1);   // Deletes last comma ","
        queryBuilder.append(") VALUES (?");
        for (int i = 0; i < this.atributos.size() - 1; i++) {   // as there's a ? before, it's necessary to decrement the
            // size by one
            queryBuilder.append(",?");
        }
        queryBuilder.append(");");
        query = queryBuilder.toString();
    }


    /**
     * Inserts all the given elements into the database. When following
     * foreign keys while inserting; It won't go into infinite recursion.
     * At most It will propperly insert a foreign key from a foreign key. For
     * example: A post wich has a parent wich is partially identified by its
     * user which is actually a custom declared Class, not a String or an
     * integer.
     */
    public void insert() throws Exception {
        Map<String, Object> insertion;
        Class fieldClass;
        String columnName;
        Object atrib;

        // Configures the connection to the database
        configureConnection();

        try {
            for (E element : this.insertions) {
                insertion = new HashMap<>();
                for (Field field : this.mappedClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(MapperColumn.class)) {
                        field.setAccessible(true);
                        fieldClass = field.getAnnotation(MapperColumn.class).targetClass();
                        if (fieldClass == Object.class) { // Normal field
                            columnName = extractColumnName(field);
                            atrib = field.get(element);
                            // Checks for default values
                            if (field.getAnnotation(MapperColumn.class).hasDefault() && atrib == null) {
                                insertion.put(columnName, new Mapper.DEFAULT());
                            } else {
                                insertion.put(columnName, atrib);
                            }
                        } else { // Foreign keys
                            if (field.get(element) != null) {   // Asumes that a foreign key can't be null
                                insertion.putAll(getFKs(element));
                            }
                        }
                    }
                }
                this.customInsertion(insertion, mappedClass.getAnnotation(MapperTable.class).name());
            }
        } catch (IllegalAccessException ex) {
            throw new Exception(ex.getMessage());
        }
    }


    /**
     * Generates a Map to be used as a template in custom insertions performed
     * by the custom insertion mapper.
     *
     * @return Map template
     */
    public static Map<String, Object> customInsertionTemplate() {
        HashMap<String, Object> insertionTemplate = new HashMap<>();

        insertionTemplate.put("table", "");
        insertionTemplate.put("set", new ArrayList<Map<String, Object>>());
        insertionTemplate.put("where", new ArrayList<Map<String, Object>>());

        return insertionTemplate;
    }

    /**
     * Automatically inserts values indicated by a list containing maps with
     * the following structure:
     * <p>
     * [
     * {
     * "Column name" : "value"
     * },
     * {
     * "Column name that has an Integer" : 445
     * },
     * {
     * "Column name that has a default value" : new Mapper.DEFAULT()
     * }
     * ]
     *
     * @param insertions Map list to be used as a guide to perform the
     *                   insertions
     */
    public void customInsertion(List<Map<String, Object>> insertions, String table) throws Exception {
        for (Map<String, Object> insertion : insertions) {
            customInsertion(insertion, table);
        }
    }

    public void customInsertion(Map<String, Object> insertion, String table) throws Exception {
        /*
         *
         * INSERT INTO post (author, id, text, publicationDate, sugarDaddy, authorDaddy, multimedia)
         * values ('id2', 'post2', 'Respuesta', default, 'post2', 'id2', null)
         *
         * */

        ArrayList<Object> params = new ArrayList<>();
        StringBuilder insertionBuilder = new StringBuilder("INSERT INTO ");
        StringBuilder valueBuilder = new StringBuilder("values (");
        insertionBuilder.append(table).append(" (");

        insertion.forEach((s, o) -> {
            if (o instanceof Mapper.DEFAULT) {
                insertionBuilder.append(s).append(",");
                valueBuilder.append("default,");
            } else {
                insertionBuilder.append(s).append(",");
                valueBuilder.append("?,");
                params.add(o);
            }

        });
        insertionBuilder.delete(insertionBuilder.length() - 1, insertionBuilder.length()).append(") ");
        valueBuilder.delete(valueBuilder.length() - 1, valueBuilder.length()).append(")");

        try {
            PreparedStatement statement = connection.prepareStatement(insertionBuilder.toString() + valueBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            statement.execute();
        } catch (SQLException sql) {
            throw new Exception(sql.getMessage());
        }
    }
}
