package mx.florinda.cardapio.client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServidorItensCardapio {

    public static void main(String[] args) throws IOException {
        var inetSocketAddress = new InetSocketAddress(8000);
        var httpServer = HttpServer.create(inetSocketAddress, 0);

        httpServer.createContext("/itensCardapio.json", exchange -> {
            var path = Path.of("itensCardapio.json");
            var json = Files.readString(path);
            var bytes = json.getBytes();

            var responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Content-type", "application/json; charset=UTF-8");

            exchange.sendResponseHeaders(200, bytes.length);

            var responseBody = exchange.getResponseBody();
            responseBody.write(bytes);
        });

        System.out.println("Subiu servidor http!");
        httpServer.start();
    }
}
