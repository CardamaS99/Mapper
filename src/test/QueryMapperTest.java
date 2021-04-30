package test;

import mapper.QueryMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import test.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class QueryMapperTest {

    private Connection connection;

    @BeforeEach
    void setUp() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String database = "db";
            String username = "root";
            String password = "password";

            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + database, username,
                    password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest(name = "[{index}]: Usuario {0}, contraseña {1}, nombre {2}, trabajo {3}")
    @CsvSource({"juanf,1234,JuanFernando,3"})
    @DisplayName("Comprobar existencia de un usuario")
    void findfirstTest(String usernameExpected, String passwordExpected, String nameExpected, int idExpected)
            throws Exception {
        /* ARRANGE */
        QueryMapper<User> mapper = new QueryMapper<>(connection);
        mapper.defineClass(User.class);

        /* ACT */

        // Se calcula el tiempo
        User user = mapper.createQuery("SELECT * FROM Person WHERE username=?").defineParameters(usernameExpected)
                .findFirst();


        /* ASSERT */
        assertEquals(user.getUsername(), usernameExpected);
        assertEquals(user.getPassword(), passwordExpected);
        assertEquals(user.getName(), nameExpected);
        assertEquals(user.getJob().getId(), idExpected);
    }


    @AfterEach
    void tearDown() throws SQLException {
        this.connection.close();
    }
}