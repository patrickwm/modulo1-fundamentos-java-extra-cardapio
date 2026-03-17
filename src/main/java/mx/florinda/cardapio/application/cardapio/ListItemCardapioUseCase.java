package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.ItemCardapio;

import java.util.List;
import java.util.logging.Logger;

public class ListItemCardapioUseCase extends GetDataItemCardapio<String, List<ItemCardapio>> {
    private static final Logger logger = Logger.getLogger(ListItemCardapioUseCase.class.getName());

    public ListItemCardapioUseCase() {
        super();
    }

    @Override
    public String getAccept(String data) {
        return data;
    }

    @Override
    public List<ItemCardapio> getData(String data) {
        return database.listarItensCardapio();
    }
}
