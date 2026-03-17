package mx.florinda.cardapio.application;

public interface UseCaseWithOutOutput<INPUT> {
    void execute(INPUT input);
}