package mx.florinda.cardapio;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public class TestesLocale {

    public static void main(String[] args) {
        Locale.availableLocales().forEach(System.out::println);

        System.out.println("Default locale: " + Locale.getDefault());

        var localeUS = Locale.US;
        var localePtBR = Locale.of("pt", "BR");

        var formatterDataHora = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
        System.out.println(formatterDataHora.format(ZonedDateTime.now()));
        System.out.println(formatterDataHora.withLocale(localeUS).format(ZonedDateTime.now()));
        System.out.println(formatterDataHora.withLocale(localePtBR).format(ZonedDateTime.now()));

        var formatterMesAno = DateTimeFormatter.ofPattern("MMMM/yyyy");
        System.out.println(formatterMesAno.format(YearMonth.now()));
        System.out.println(formatterMesAno.withLocale(localeUS).format(YearMonth.now()));
        System.out.println(formatterMesAno.withLocale(localePtBR).format(YearMonth.now()));

        System.out.println(NumberFormat.getCurrencyInstance().format(2.99));
        System.out.println(NumberFormat.getCurrencyInstance(localeUS).format(2.99));
        System.out.println(NumberFormat.getCurrencyInstance(localePtBR).format(2.99));

        var mensagens = ResourceBundle.getBundle("mensagens");
        var mensagensUS = ResourceBundle.getBundle("mensagens", localeUS);
        var mensagensPtBR = ResourceBundle.getBundle("mensagens", localePtBR);

        System.out.println(mensagens.getString("categoria.cardapio.pratos_principais"));
        System.out.println(mensagensUS.getString("categoria.cardapio.pratos_principais"));
        System.out.println(mensagensPtBR.getString("categoria.cardapio.pratos_principais"));

    }

}
