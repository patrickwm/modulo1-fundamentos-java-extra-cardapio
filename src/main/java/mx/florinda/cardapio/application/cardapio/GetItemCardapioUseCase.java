package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.ItemCardapio;
import mx.florinda.cardapio.application.exception.NotFoundException;

import java.util.logging.Logger;

public class GetItemCardapioUseCase extends GetDataItemCardapio<GetItemCardapioUseCase.ItemCardapioSearch, ItemCardapio> {
    private static final Logger logger = Logger.getLogger(GetItemCardapioUseCase.class.getName());

    public GetItemCardapioUseCase() {
        super();
    }

    @Override
    public String getAccept(ItemCardapioSearch itemCardapioSearch) {
        return itemCardapioSearch.accept();
    }

    @Override
    public ItemCardapio getData(ItemCardapioSearch itemCardapioSearch) {
        var itemCardapioOpt =  database.itemCardapioPorId(itemCardapioSearch.id());

        if (itemCardapioOpt.isEmpty()) {
            throw new NotFoundException("Item cardápio " +  itemCardapioSearch.id + " não encontrado.");
        }

        return itemCardapioOpt.get();
    }

    public record ItemCardapioSearch(String accept, long id) {
    }
}
