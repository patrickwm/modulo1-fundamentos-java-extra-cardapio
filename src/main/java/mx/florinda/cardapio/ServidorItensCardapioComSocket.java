package mx.florinda.cardapio;

import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServidorItensCardapioComSocket {

    private static final Logger logger = Logger.getLogger(ServidorItensCardapioComSocket.class.getName());

    private static final Database database = new SQLDatabase();

    public static void main(String[] args) throws Exception {

        Executor executor = Executors.newFixedThreadPool(50);

        try(ServerSocket serverSocket = new ServerSocket(8000)) {
            logger.info("Subiu servidor!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(() -> trataRequisicao(clientSocket));
            }

        }
    }

    private static void trataRequisicao(Socket clientSocket) {

        try (clientSocket) {
            InputStream clientIS = clientSocket.getInputStream();

            StringBuilder requestBuilder = new StringBuilder();
            int data;
            do {
                data = clientIS.read();
                requestBuilder.append((char) data);
            } while (clientIS.available() > 0);

            String request = requestBuilder.toString();
            logger.finest(request);
            logger.fine("\n\nChegou um novo request");

            String[] requestChunks = request.split("\r\n\r\n");
            String requestLineAndHeaders = requestChunks[0];
            String[] requestLineAndHeadersChunks = requestLineAndHeaders.split("\r\n");
            String requestLine = requestLineAndHeadersChunks[0];
            String[] requestLineChunks = requestLine.split(" ");
            String method = requestLineChunks[0];
            String requestURI = requestLineChunks[1];
            String httpVersion = requestLineChunks[2];

            logger.finer(() -> "Method: " + method);
            logger.finer(() -> "Request URI: " + requestURI);
            logger.finer(() -> "HTTP Version: " + httpVersion);

            Thread.sleep(250);

            OutputStream clientOS = clientSocket.getOutputStream();
            PrintStream clientOut = new PrintStream(clientOS);

            try {
                if ("/itensCardapio.json".equals(requestURI)) {
                    logger.fine("Chamou arquivo itensCardapio.json");

                    Path path = Path.of("itensCardapio.json");
                    String json = Files.readString(path);

                    clientOut.println("HTTP/1.1 200 OK");
                    clientOut.println("Content-type: application/json; charset=UTF-8");
                    clientOut.println();
                    clientOut.println(json);

                } else if ("GET".equals(method) && "/itens-cardapio".equals(requestURI)) {
                    logger.fine("Chamou listagem de itens de cardápio");
                    List<ItemCardapio> listaItensCardapio = database.listaItensCardapio();

                    String mediaType = "application/json";
                    for (int i = 1; i < requestLineAndHeadersChunks.length; i++) {
                        String header = requestLineAndHeadersChunks[i];
                        if (header.contains("Accept")) {
                            logger.info(header);
                            mediaType = header.replace("Accept: ", "");
                        }
                    }

                    byte[] body;
                    if ("application/x-java-serialized-object".equals(mediaType)) {
                        logger.info("Enviando objeto java serializado");
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(listaItensCardapio);
                        body = bos.toByteArray();
                    } else {
                        Gson gson = new Gson();
                        String json = gson.toJson(listaItensCardapio);
                        body = json.getBytes(StandardCharsets.UTF_8);
                    }

                    clientOS.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                    clientOS.write(("Content-type: " + mediaType + "; charset=UTF-8\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    clientOS.write(body);
                    clientOS.flush();
                } else if ("GET".equals(method) && "/itens-cardapio/total".equals(requestURI)) {
                    logger.fine("Chamou total de itens de cardápio");
                    int totalItens = database.totalItensCardapio();

                    clientOut.println("HTTP/1.1 200 OK");
                    clientOut.println();
                    clientOut.println(totalItens);
                } else if ("POST".equals(method) && "/itens-cardapio".equals(requestURI)) {
                    logger.fine("Chamou adição de itens de cardápio");

                    if (requestChunks.length == 1) {
                        clientOut.println("HTTP/1.1 400 Bad Request");
                    }
                    String body = requestChunks[1];

                    Gson gson = new Gson();
                    ItemCardapio item = gson.fromJson(body, ItemCardapio.class);

                    database.adicionaItemCardapio(item);

                    clientOut.println("HTTP/1.1 200 OK");
                } else if ("GET".equals(method) && ("/".equals(requestURI) || "/en".equals(requestURI))) {
                    logger.fine("Chamou página raiz");

                    List<ItemCardapio> listaItensCardapio = database.listaItensCardapio();

                    Locale locale = "/en".equals(requestURI) ? Locale.US : Locale.of("pt", "BR");
                    NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(locale);
                    ResourceBundle mensagens = ResourceBundle.getBundle("mensagens", locale);
                    DateTimeFormatter formatterDataHora = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                            .withLocale(locale);
                    DateTimeFormatter formatterMesAno = DateTimeFormatter.ofPattern("MMMM/yyyy")
                            .withLocale(locale);

                    StringBuilder htmlTodosItens = new StringBuilder();
                    for (ItemCardapio item : listaItensCardapio) {
                        String htmlPrecoItem;
                        if (item.precoPromocional() == null) {
                            htmlPrecoItem = "<strong>" + formatadorMoeda.format(item.preco()) + "</strong>";
                        } else {
                            htmlPrecoItem = "<mark>Em promoção</mark> <strong>" + formatadorMoeda.format(item.precoPromocional()) + "</strong> <s>" + formatadorMoeda.format(item.preco()) + "</s>";
                        }

                        String categoria = mensagens.getString("categoria.cardapio." + item.categoria().name().toLowerCase());

                        String htmlItem = """
                                    <article>
                                        <kbd>%s</kbd>
                                        <h3>%s</h3>
                                        <p>%s</p>
                                        %s
                                    </article>
                                """.formatted(categoria, item.nome(), item.descricao(), htmlPrecoItem);
                        htmlTodosItens.append(htmlItem);
                    }


                    String html = """
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <title>Florinda Eats - Cardápio</title>
                                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2.1.1/css/pico.min.css">
                            </head>
                            <body>
                            
                            <header class="container">
                                <hgroup>
                                    <h1>Florinda Eats</h1>
                                    <p>O sabor da Vila direto pra você</p>
                                </hgroup>
                            </header>
                            
                            <main class="container">
                                <h2>Cardápio</h2>
                            
                            %s
                            
                            </main>
                        
                            <footer class="container">
                                <p><small><em>Preços de acordo com %s</em></small></p>
                                <p><strong>Florinda Eats</strong> Todos os direitos reservados - %s</p>
                            </footer>
                            </body>
                            </html>
                            """.formatted(htmlTodosItens.toString(), formatterDataHora.format(ZonedDateTime.now())
                                                                   , formatterMesAno.format(YearMonth.now()));

                    clientOut.print("HTTP/1.1 200 OK\r\n");
                    clientOut.print("Content-type: text/html; charset=UTF-8\r\n\r\n");
                    clientOut.print(html);
                    clientOut.print("\r\n");
                } else {
                    logger.warning("URI não encontrada: " + requestURI);
                    clientOut.println("HTTP/1.1 404 Not Found");
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex, () -> "Erro ao tratar " + method + " " + requestURI);
                clientOut.println("HTTP/1.1 500 Internal Server Error");
            }

        } catch (Exception ex) {
            //  logger.severe("Erro no servidor");
            logger.log(Level.SEVERE, "Erro fatal no servidor", ex);
            throw new RuntimeException(ex);
        }
    }

}
