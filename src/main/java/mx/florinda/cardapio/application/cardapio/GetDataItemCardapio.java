package mx.florinda.cardapio.application.cardapio;

import com.google.gson.Gson;
import mx.florinda.cardapio.application.UseCase;
import mx.florinda.cardapio.database.Database;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public abstract class GetDataItemCardapio<INPUT, TYPE_DATA> implements UseCase<INPUT, GetDataItemCardapio.DataItemCardapio> {
    private static final Logger logger = Logger.getLogger(GetDataItemCardapio.class.getName());
    protected final Database database;

    public GetDataItemCardapio() {
        this.database = Database.getInstance();
    }

    @Override
    public DataItemCardapio execute(INPUT data) {
        var listaItensCardapio = getData(data);
        var serializationType = "application/x-java-serialized-object".equals(getAccept(data)) ?
                new JavaSerialization() : new JsonSerialization();
        return serializationType.serialize(listaItensCardapio);
    }

    public abstract String getAccept(INPUT data);

    public abstract TYPE_DATA getData(INPUT data);

    private interface SerializationType<TYPE_DATA> {
        DataItemCardapio serialize(TYPE_DATA items);
    }

    private class JavaSerialization implements GetDataItemCardapio.SerializationType<TYPE_DATA> {
        @Override
        public DataItemCardapio serialize(TYPE_DATA items) {
            logger.info("Enviando objeto java serializado");
            try (var bos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(items);
                return new DataItemCardapio("application/x-java-serialized-object", bos.toByteArray());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class JsonSerialization implements GetDataItemCardapio.SerializationType<TYPE_DATA> {
        @Override
        public DataItemCardapio serialize(TYPE_DATA items) {
            var gson = new Gson();
            var json = gson.toJson(items);
            return new DataItemCardapio("application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    public record DataItemCardapio(String mediaType, byte[] body) {
    }
}
