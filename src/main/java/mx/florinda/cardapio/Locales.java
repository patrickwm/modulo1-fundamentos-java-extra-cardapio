package mx.florinda.cardapio;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public class Locales {
    public static void main(String[] args) {
        Locale.availableLocales().forEach(System.out::println);

        System.out.println("Default locale: " + Locale.getDefault());

        Locale localePtBR = Locale.of("pt", "BR");
        Locale localeUS = Locale.US;

        DateTimeFormatter formatadorDataHora = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        System.out.println(formatadorDataHora.format(ZonedDateTime.now()));  // Aug 30, 2025, 5:17:55 PM
        System.out.println(formatadorDataHora.withLocale(localePtBR).format(ZonedDateTime.now())); // 30 de ago. de 2025 17:17:55
        System.out.println(formatadorDataHora.withLocale(localeUS).format(ZonedDateTime.now()));  // Aug 30, 2025, 5:17:55 PM

        DateTimeFormatter formatadorAnoMes = DateTimeFormatter.ofPattern("MMMM/yyyy");
        System.out.println(formatadorAnoMes.format(YearMonth.now()));
        System.out.println(formatadorAnoMes.withLocale(localePtBR).format(YearMonth.now()));
        System.out.println(formatadorAnoMes.withLocale(localeUS).format(YearMonth.now()));

        System.out.println(NumberFormat.getCurrencyInstance().format(2.99));
        System.out.println(NumberFormat.getCurrencyInstance(localePtBR).format(2.99));
        System.out.println(NumberFormat.getCurrencyInstance(localeUS).format(2.99));

        ResourceBundle mensagens = ResourceBundle.getBundle("mensagens");
        System.out.println(mensagens.getString("categoria.cardapio.pratos_principais"));

    }
}
