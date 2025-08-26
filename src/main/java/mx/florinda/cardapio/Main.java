package mx.florinda.cardapio;


import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Database database = new SQLDatabase();

        List<ItemCardapio> listaItensCardapio = database.listaItensCardapio();
        listaItensCardapio.forEach(System.out::println);

        int total = database.totalItensCardapio();
        System.out.println(total);

        var novoItemCardapio = new ItemCardapio(10L, "Tacos de Carnitas", "Incríveis tacos recheados com carne tenra", ItemCardapio.CategoriaCardapio.PRATOS_PRINCIPAIS, new BigDecimal("25.9"), null);
        database.adicionaItemCardapio(novoItemCardapio);
    }

}
