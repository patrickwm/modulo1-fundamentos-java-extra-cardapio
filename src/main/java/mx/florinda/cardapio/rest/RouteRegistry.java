package mx.florinda.cardapio.rest;

import mx.florinda.cardapio.rest.annotatios.HttpMethod;
import mx.florinda.cardapio.rest.annotatios.Rest;
import mx.florinda.cardapio.rest.annotatios.methods.Path;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RouteRegistry {
    private static final Logger logger = Logger.getLogger(RouteRegistry.class.getName());
    private static final List<RouteDefinition> routes = new ArrayList<>();

    public static void initialize() {
        logger.info("Inicializando o registro de rotas");
        var reflection = new Reflections("mx.florinda.cardapio");

        reflection.getTypesAnnotatedWith(Rest.class)
            .forEach(RouteRegistry::addClassRouteDefinition);
        logger.info("Registro de rotas finalizado");
        logger.fine(() -> "Total de rotas registradas: " + routes.size());
    }

    private static void addClassRouteDefinition(Class<?> clazz) {
        logger.fine(() -> "Validando classe " + clazz.getName() + " para adicionar ao registro de rotas");
        Arrays.stream(clazz.getMethods())
            .forEach(m -> addMethodRouteDefinition(clazz, m));
    }

    private static void addMethodRouteDefinition(Class<?> clazz, Method method) {
        logger.fine(() -> "Validando método " + clazz.getName() + ":" + method.getName() + " para adicionar ao ao registro de rotas");
        if (!method.isAnnotationPresent(Path.class)) {
            logger.fine(() -> "Método " + clazz.getName() + ":" + method.getName() + " não possui o @Path");
            return;
        }

        var httpMethodsAnnotations = Arrays.stream(method.getAnnotations())
            .filter(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))
            .toList();

        if (httpMethodsAnnotations.isEmpty()) {
            logger.fine(() -> "Método " + clazz.getName() + ":" + method.getName() + " não está anotado com algum método HTTP");
            return;
        }

        if (httpMethodsAnnotations.size() > 1) {
            var methods = httpMethodsAnnotations.stream()
                .map(a -> a.annotationType().getName()).collect(Collectors.joining(", "));

            throw new IllegalStateException("O método " + clazz.getName() + ":" + method.getName() + " possuí mais de um método HTTP: " + methods);
        }

        var httpMethod = httpMethodsAnnotations.stream()
            .findFirst()
            .map(a -> a.annotationType().getSimpleName())
            .orElseThrow();

        Arrays.stream(method.getAnnotation(Path.class).value())
            .map(path -> new RouteDefinition(method, httpMethod, path))
            .peek(path -> logger.fine(() -> "Adicionando " + clazz.getName() + ":" + method.getName() + " ao registro de rotas"))
            .forEach(routes::add);
    }

    public static List<RouteDefinition> searchExecutionMethod(String method, String requestURI) {
        return RouteRegistry.routes
            .stream()
            .filter(r -> r.isMethod(method, requestURI))
            .toList();
    }


}
