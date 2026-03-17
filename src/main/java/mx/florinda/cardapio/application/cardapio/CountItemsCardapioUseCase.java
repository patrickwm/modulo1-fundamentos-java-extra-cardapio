package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.application.UseCaseWithOutInput;
import mx.florinda.cardapio.database.Database;

public class CountItemsCardapioUseCase implements UseCaseWithOutInput<CountItemsCardapioUseCase.Total> {
    private final Database database;

    public CountItemsCardapioUseCase() {
        this.database = Database.getInstance();
    }

    @Override
    public Total execute() {
        return new Total(database.totalItensCardapio());
    }

    public record Total(int total) {
    }
}
