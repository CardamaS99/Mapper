package mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that the Fields has to be mapped when using the database
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapperColumn {
    String column() default "";                     // Column name

    boolean pkey() default false;                   // True if it's a primary key

    boolean hasDefault() default false;             // Has a default value on the definition

    String fKeys() default "";                      // The name of the foreign keys of the reference
    // Syntax: "columnNameAsFK:columnNameAsPK"

    Class<?> targetClass() default Object.class;    // It's actually a Mappeable class (foreign keys); Object class
                                                    // cannot act as FK because all checks are made against Object
                                                    // as targetClass cannot be null

    boolean notNull() default false;                // True if the object can not be null
}
