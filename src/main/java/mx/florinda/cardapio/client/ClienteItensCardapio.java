package mx.florinda.cardapio.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClienteItensCardapio {

    public static void main(String[] args) throws Exception {
        var uri = URI.create("http://localhost:8000/itens-cardapio");

        try (var httpClient = HttpClient.newHttpClient()) {
            var httpRequest = HttpRequest.newBuilder(uri).build();
            var httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            var statusCode = httpResponse.statusCode();
            var body = httpResponse.body();
            System.out.println(statusCode);
            System.out.println(body);
        }
    }
}
