package mx.florinda.cardapio.infra;

import java.io.IOException;
import java.util.Properties;

public class Propriedades {
    private final Properties props;

    public Propriedades() {
        var props = new Properties();

        try (var is = Propriedades.class.getClassLoader().getResourceAsStream("cardapio.properties")) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.props = props;
    }

    public String get(String key) {
        return props.getProperty(key);
    }
}
