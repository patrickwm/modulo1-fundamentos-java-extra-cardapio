package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.application.UseCaseWithOutOutput;
import mx.florinda.cardapio.application.exception.NotFoundException;
import mx.florinda.cardapio.database.Database;

import java.math.BigDecimal;
import java.util.logging.Logger;

public class AlterPriceItemCardapioUseCase implements UseCaseWithOutOutput<AlterPriceItemCardapioUseCase.AlterPrice> {
    private static final Logger logger = Logger.getLogger(AlterPriceItemCardapioUseCase.class.getName());
    private final Database database;

    public AlterPriceItemCardapioUseCase() {
        this.database = Database.getInstance();
    }

    @Override
    public void execute(AlterPrice alterPrice) {
        var altered = this.database.alterarPrecoItemCardapio(alterPrice.id(), alterPrice.price());

        logger.info(() -> "Item cardápio " + alterPrice.id() + " teve preço alterado para: " + alterPrice.price());

        if (!altered) {
            throw new NotFoundException("Não foi encontrado o item de cardápio " + alterPrice.id());
        }
    }

    public record AlterPrice(long id, BigDecimal price) {
    }
}
