package mx.florinda.cardapio;

import com.google.gson.Gson;
import mx.florinda.cardapio.database.InMemoryDatabase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeradorItensCardapioJSON {

    public static void main(String[] args) throws IOException {
        var database = new InMemoryDatabase();
        var listaItensCardapio = database.listaItensCardapio();

        var gson = new Gson();
        var json = gson.toJson(listaItensCardapio);

        var path = Path.of("itensCardapio.json");
        Files.writeString(path, json);

    }

}
