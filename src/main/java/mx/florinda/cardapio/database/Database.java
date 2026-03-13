package mx.florinda.cardapio.database;

import mx.florinda.cardapio.ItemCardapio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface Database {
    List<ItemCardapio> listaItensCardapio();

    Optional<ItemCardapio> itemCardapioPorId(Long id);

    boolean removeItemCardapio(Long id);

    boolean alteraPrecoItemCardapio(Long id, BigDecimal novoPreco);

    int totalItensCardapio();

    void adicionaItemCardapio(ItemCardapio item);
}
