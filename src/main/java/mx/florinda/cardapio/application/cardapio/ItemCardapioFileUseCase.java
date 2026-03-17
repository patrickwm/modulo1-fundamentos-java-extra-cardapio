package mx.florinda.cardapio.application.cardapio;

import mx.florinda.cardapio.application.UseCase;
import mx.florinda.cardapio.application.exception.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ItemCardapioFileUseCase implements UseCase<String, String> {
    @Override
    public String execute(String filePath) {
        var path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new NotFoundException("Arquivo " + filePath + " não existe.");
        }

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
