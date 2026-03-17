package mx.florinda.cardapio.application;

public interface UseCase<INPUT, OUTPUT> {
    OUTPUT execute(INPUT input);
}
