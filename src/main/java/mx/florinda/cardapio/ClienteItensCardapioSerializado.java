package mx.florinda.cardapio;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ClienteItensCardapioSerializado {

    public static void main(String[] args) throws Exception {

        URI uri = URI.create("http://localhost:8000/itens-cardapio");

        try(HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/x-java-serialized-object")
                    .build();
            HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest,
                                                            HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = httpResponse.statusCode();
            byte[] body = httpResponse.body();
            System.out.println(statusCode);
            System.out.println(body);

            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            ObjectInputStream ois = new ObjectInputStream(bis);
            List<ItemCardapio> itens = (List<ItemCardapio>) ois.readObject();
            itens.forEach(System.out::println);
        }

    }
}
