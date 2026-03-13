package mx.florinda.cardapio;


import mx.florinda.cardapio.database.SQLDatabase;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        var database = new SQLDatabase();

        var listaItensCardapio = database.listaItensCardapio();
        listaItensCardapio.forEach(System.out::println);

        var total = database.totalItensCardapio();
        System.out.println(total);

        var novoItemCardapio = new ItemCardapio(10L, "Tacos de Carnitas", "Incríveis tacos recheados com carne tenra", ItemCardapio.CategoriaCardapio.PRATOS_PRINCIPAIS, new BigDecimal("25.9"), null);
        database.adicionaItemCardapio(novoItemCardapio);
    }

}
