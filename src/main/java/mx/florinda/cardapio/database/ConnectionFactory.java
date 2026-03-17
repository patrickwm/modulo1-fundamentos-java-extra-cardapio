package mx.florinda.cardapio.database;

import mx.florinda.cardapio.infra.Propriedades;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private static final ConnectionFactory INSTANCE = new ConnectionFactory();
    private final Propriedades propriedades = new Propriedades();
    private final String jdbcUrl = propriedades.get("db.url");
    private final String user = propriedades.get("db.user");
    private final String password = propriedades.get("db.password");

    private ConnectionFactory() {
    }

    public static ConnectionFactory getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }
}
