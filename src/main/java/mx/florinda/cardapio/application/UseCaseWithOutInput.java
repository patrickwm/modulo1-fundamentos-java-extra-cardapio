package mx.florinda.cardapio.application;

public interface UseCaseWithOutInput<OUTPUT> {
    OUTPUT execute();
}