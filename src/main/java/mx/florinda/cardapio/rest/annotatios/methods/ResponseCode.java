package mx.florinda.cardapio.rest.annotatios.methods;

import mx.florinda.cardapio.rest.HttpStatus;
import mx.florinda.cardapio.rest.annotatios.ErrorMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseCode {
    HttpStatus success() default HttpStatus.OK;
    ErrorMapping[] fail() default {};
}
