package mx.florinda.cardapio.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClienteViaCEP {

    public static void main(String[] args) throws Exception {

        // URL url = new URL("https://viacep.com.br/ws/01001000/json/");

        var uri = URI.create("https://viacep.com.br/ws/01001000/json/");


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
