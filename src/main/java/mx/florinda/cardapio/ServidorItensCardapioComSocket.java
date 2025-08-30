package mx.florinda.cardapio;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.format.DateTimeFormatter.ofPattern;

public class ServidorItensCardapioComSocket {

    private static final Locale localePtBR = Locale.of("pt", "BR");
    private static final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(localePtBR);
    private static final DateTimeFormatter formadorDataHora = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(localePtBR);
    private static final DateTimeFormatter formatadorAnoMes = DateTimeFormatter.ofPattern("MMMM/yyyy").withLocale(localePtBR);
    private static final ResourceBundle mensagens = ResourceBundle.getBundle("mensagens");

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

                    Gson gson = new Gson();
                    String json = gson.toJson(listaItensCardapio);

                    clientOut.println("HTTP/1.1 200 OK");
                    clientOut.println("Content-type: application/json; charset=UTF-8");
                    clientOut.println();
                    clientOut.println(json);
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
                } else if ("GET".equals(method) && "/".equals(requestURI)) {
                    logger.fine("Chamou página raiz");

                    List<ItemCardapio> listaItensCardapio = database.listaItensCardapio();

                    StringBuilder itemHtmlBuilder = new StringBuilder();

                    for (ItemCardapio item : listaItensCardapio) {
                        String categoria = mensagens.getString("categoria.cardapio."+item.categoria().name().toLowerCase());
                        itemHtmlBuilder.append("<article>");
                        itemHtmlBuilder.append("<kbd>").append(categoria).append("</kbd>");
                        itemHtmlBuilder.append("<h3>").append(item.nome()).append("</h3>");
                        itemHtmlBuilder.append("<p>").append(item.descricao()).append("</p>");

                        if (item.precoPromocional() == null) {
                            itemHtmlBuilder.append("<strong>").append(formatadorMoeda.format(item.preco())).append("</strong>");
                        } else {
                            itemHtmlBuilder
                                    .append("<mark>Em promoção</mark> <strong>")
                                                    .append(formatadorMoeda.format(item.precoPromocional())).append("</strong>")
                                    .append(" <s>").append(formatadorMoeda.format(item.preco())).append("</s>");
                        }
                        itemHtmlBuilder.append("</article>");
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
                            """.formatted(itemHtmlBuilder.toString(), formadorDataHora.format(LocalDateTime.now()), formatadorAnoMes.format(YearMonth.now()));

                    clientOut.print("HTTP/1.1 200 OK\r\n");
                    clientOut.println("Content-type: text/html; charset=UTF-8");
                    clientOut.print("\r\n");
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
