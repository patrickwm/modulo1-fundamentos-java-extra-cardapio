package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.application.UseCase;
import mx.florinda.cardapio.database.Database;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public class PageRootUseCase implements UseCase<String, String> {
    private final Database database;

    public PageRootUseCase() {
        this.database = Database.getInstance();
    }

    @Override
    public String execute(String uri) {
        var listaItensCardapio = database.listarItensCardapio();
        var locale = "/en".equals(uri) ? Locale.US : Locale.of("pt", "BR");
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


        return """
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
    }
}
