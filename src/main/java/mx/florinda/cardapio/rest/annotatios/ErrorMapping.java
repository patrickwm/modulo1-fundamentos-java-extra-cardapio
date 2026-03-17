package mx.florinda.cardapio.rest.annotatios;

import mx.florinda.cardapio.rest.HttpStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ErrorMapping {
    Class<? extends Throwable> exception();
    HttpStatus status();
}
