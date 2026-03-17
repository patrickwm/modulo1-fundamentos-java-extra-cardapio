package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.ItemCardapio;
import mx.florinda.cardapio.application.UseCaseWithOutOutput;
import mx.florinda.cardapio.database.Database;

public class CreateItemCardapioUseCase implements UseCaseWithOutOutput<ItemCardapio> {
    private final Database database;

    public CreateItemCardapioUseCase() {
        this.database = Database.getInstance();
    }

    @Override
    public void execute(ItemCardapio itemCardapio) {
        if (itemCardapio == null) {
            throw new IllegalArgumentException("Item cardápio não pode ser nulo.");
        }

        database.adicionarItemCardapio(itemCardapio);
    }
}
