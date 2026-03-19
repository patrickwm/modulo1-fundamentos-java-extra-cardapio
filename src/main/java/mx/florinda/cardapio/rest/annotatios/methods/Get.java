package mx.florinda.cardapio.rest.annotatios.methods;

import mx.florinda.cardapio.rest.annotatios.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod
public @interface Get {
}
