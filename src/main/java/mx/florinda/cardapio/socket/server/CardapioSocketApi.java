package mx.florinda.cardapio.socket.server;

import com.google.gson.Gson;
import mx.florinda.cardapio.database.Database;
import mx.florinda.cardapio.ItemCardapio;
import mx.florinda.cardapio.database.SQLDatabase;
import mx.florinda.cardapio.socket.server.rest.ClientOS;
import mx.florinda.cardapio.socket.server.rest.Get;
import mx.florinda.cardapio.socket.server.rest.HeaderParam;
import mx.florinda.cardapio.socket.server.rest.Post;
import mx.florinda.cardapio.socket.server.rest.Path;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static mx.florinda.cardapio.Params.CRLF;

public class CardapioSocketApi {
    private static final Logger logger = Logger.getLogger(CardapioSocketApi.class.getName());
    private static final Database database = new SQLDatabase();

    @Get
    @Path("/itens-cardapio")
    public void listItensCardapio(@HeaderParam("Accept") String accept, @ClientOS OutputStream clientOS)
            throws IOException {
        logger.fine("Chamou listagem de itens de cardápio");
        var listaItensCardapio = database.listaItensCardapio();
        var mediaType = "application/json";

        byte[] body;
        if ("application/x-java-serialized-object".equals(accept)) {
            mediaType = "application/x-java-serialized-object";
            logger.info("Enviando objeto java serializado");
            try (var bos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(listaItensCardapio);
                body = bos.toByteArray();
            }
        } else {
            var gson = new Gson();
            var json = gson.toJson(listaItensCardapio);
            body = json.getBytes(StandardCharsets.UTF_8);
        }

        var responseLine = "HTTP/1.1 200 OK" + CRLF;
        var responseContentType = "Content-type: %s; charset=UTF-8%s%s".formatted(mediaType, CRLF, CRLF);

        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
        clientOS.write(responseContentType.getBytes(StandardCharsets.UTF_8));
        clientOS.write(body);
        clientOS.flush();
    }

    @Get
    @Path("/itens-cardapio/total")
    public void countItensCardapio(@ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou total de itens de cardápio");
        var totalItens = database.totalItensCardapio();

        var responseLine = "HTTP/1.1 200 OK" + CRLF;
        var responseContentType = "Content-type: application/json; charset=UTF-8%s%s" + CRLF + CRLF;
        var body = "{ \"total\": %d }".formatted(totalItens);

        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
        clientOS.write(responseContentType.getBytes(StandardCharsets.UTF_8));
        clientOS.write(body.getBytes(StandardCharsets.UTF_8));
        clientOS.flush();
    }

    @Get
    @Path("/itensCardapio.json")
    public void fileItensCardapio(@ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou arquivo itensCardapio.json");

        var path = java.nio.file.Path.of("itensCardapio.json");
        var json = Files.readString(path);
        var responseLine = "HTTP/1.1 200 OK" + CRLF;
        var responseContentType = "Content-type: application/json; charset=UTF-8%s%s" + CRLF + CRLF;

        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
        clientOS.write(responseContentType.getBytes(StandardCharsets.UTF_8));
        clientOS.write(json.getBytes(StandardCharsets.UTF_8));
        clientOS.flush();
    }

    @Get
    @Path({"/", "/en"})
    public void index(RequestInfo requestInfo, @ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou página raiz");

        var listaItensCardapio = database.listaItensCardapio();

        var locale = "/en".equals(requestInfo.uri()) ? Locale.US : Locale.of("pt", "BR");
        var formatadorMoeda = NumberFormat.getCurrencyInstance(locale);
        var mensagens = ResourceBundle.getBundle("mensagens", locale);

        var formatterDataHora = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(locale);

        var formatterMesAno = DateTimeFormatter.ofPattern("MMMM/yyyy")
                .withLocale(locale);

        var htmlTodosItens = new StringBuilder();
        for (var item : listaItensCardapio) {
            String htmlPrecoItem;
            if (item.precoPromocional() == null) {
                htmlPrecoItem = "<strong>" + formatadorMoeda.format(item.preco()) + "</strong>";
            } else {
                htmlPrecoItem = "<mark>Em promoção</mark> <strong>" +
                        formatadorMoeda.format(item.precoPromocional()) + "</strong> <s>" +
                        formatadorMoeda.format(item.preco()) + "</s>";
            }

            var categoria =
                    mensagens.getString("categoria.cardapio." + item.categoria().name().toLowerCase());

            var htmlItem = """
                        <article>
                            <kbd>%s</kbd>
                            <h3>%s</h3>
                            <p>%s</p>
                            %s
                        </article>
                    """.formatted(categoria, item.nome(), item.descricao(), htmlPrecoItem);
            htmlTodosItens.append(htmlItem);
        }


        var html = """
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
                """.formatted(
                htmlTodosItens.toString(),
                formatterDataHora.format(ZonedDateTime.now()),
                formatterMesAno.format(YearMonth.now()));

        var responseLine = "HTTP/1.1 200 OK" + CRLF;
        var responseContentType = "Content-type: text/html; charset=UTF-8%s%s" + CRLF + CRLF;

        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
        clientOS.write(responseContentType.getBytes(StandardCharsets.UTF_8));
        clientOS.write(html.getBytes(StandardCharsets.UTF_8));
        clientOS.flush();
    }

    @Post
    @Path("/itens-cardapio")
    public void createItemCardapio(@ClientOS OutputStream clientOS, ItemCardapio itemCardapio) throws IOException {
        logger.fine("Chamou adição de itens de cardápio");

        if (itemCardapio == null) {
            var responseLine = "HTTP/1.1 400 Bad Request" + CRLF;
            clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));

            return;
        }

        database.adicionaItemCardapio(itemCardapio);

        var responseLine = "HTTP/1.1 201 CREATED" + CRLF;
        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
        clientOS.flush();
    }
}

