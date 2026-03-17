package mx.florinda.cardapio.database;

import mx.florinda.cardapio.ItemCardapio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface Database {
    List<ItemCardapio> listarItensCardapio();

    Optional<ItemCardapio> itemCardapioPorId(Long id);

    boolean removerItemCardapio(Long id);

    boolean alterarPrecoItemCardapio(Long id, BigDecimal novoPreco);

    int totalItensCardapio();

    void adicionarItemCardapio(ItemCardapio item);

    static Database getInstance() {
        return new SQLDatabase();
    }
}
