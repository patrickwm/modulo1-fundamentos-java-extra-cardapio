package mx.florinda.cardapio.client;

import mx.florinda.cardapio.ItemCardapio;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ClienteItensCardapioSerializado {

    public static void main(String[] args) throws Exception {

        var uri = URI.create("http://localhost:8000/itens-cardapio");

        try(var httpClient = HttpClient.newHttpClient()) {
            var httpRequest = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/x-java-serialized-object")
                    .build();
            var httpResponse = httpClient.send(
                                                                    httpRequest,
                                                                    HttpResponse.BodyHandlers.ofByteArray());
            var statusCode = httpResponse.statusCode();
            var body = httpResponse.body();
            System.out.println(statusCode);
            System.out.println(body);

            var bis = new ByteArrayInputStream(body);
            var ois = new ObjectInputStream(bis);
            var itens = (List<ItemCardapio>) ois.readObject();
            itens.forEach(System.out::println);
        }

    }
}
