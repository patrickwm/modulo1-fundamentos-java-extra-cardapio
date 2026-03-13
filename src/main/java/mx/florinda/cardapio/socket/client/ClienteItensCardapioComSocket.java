package mx.florinda.cardapio.socket.client;

import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ClienteItensCardapioComSocket {

    public static void main(String[] args) throws Exception {

        try (var socket = new Socket("localhost", 8000)) {
            var clientOS = socket.getOutputStream();
            var clientOut = new PrintStream(clientOS);
            clientOut.println("GET /itensCardapio.json HTTP/1.1");
            clientOut.println();

            var clientIS = socket.getInputStream();
            var scanner = new Scanner(clientIS);
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                System.out.println(line);
            }
        }

    }

}
