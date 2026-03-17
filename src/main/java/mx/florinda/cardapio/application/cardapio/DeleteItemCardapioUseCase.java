package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.application.UseCaseWithOutOutput;
import mx.florinda.cardapio.application.exception.NotFoundException;
import mx.florinda.cardapio.database.Database;

import java.util.logging.Logger;

public class DeleteItemCardapioUseCase implements UseCaseWithOutOutput<Long> {
    private static final Logger logger = Logger.getLogger(DeleteItemCardapioUseCase.class.getName());
    private final Database database;

    public DeleteItemCardapioUseCase() {
        this.database = Database.getInstance();
    }

    @Override
    public void execute(Long id) {
        var removed = this.database.removerItemCardapio(id);

        logger.info(() -> "Item cardápio " + id + " removido: " + removed);

        if (!removed) {
            throw new NotFoundException("Não foi encontrado o item de cardápio " + id);
        }
    }
}
