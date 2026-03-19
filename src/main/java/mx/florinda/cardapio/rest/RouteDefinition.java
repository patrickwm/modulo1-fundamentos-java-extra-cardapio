package mx.florinda.cardapio.rest;

import mx.florinda.cardapio.rest.annotatios.params.ClientOS;
import mx.florinda.cardapio.rest.annotatios.params.HeaderParam;
import mx.florinda.cardapio.rest.annotatios.params.PathParam;
import mx.florinda.cardapio.socket.server.RequestInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RouteDefinition {
    private Method method;
    private String httpMethod;
    private String path;
    private Pattern pattern;
    private Map<Integer, String> headerParams;
    private Map<Integer, Map.Entry<String, Class<?>>> pathParams;
    private Map<Integer, Parameter> pathParamsWithIndex;
    private Integer clientOSIndex;
    private Integer requestInfoIndex;
    private Map<Integer, Class<?>> objectsByIndex;
    private List<Map.Entry<Integer, Parameter>> dataParameters;

    public RouteDefinition(Method method, String httpMethod, String path) {
        this.method = method;
        this.httpMethod = httpMethod;
        this.path = path;
        this.dataParameters = IntStream.range(0, method.getParameterCount())
                .mapToObj(i -> Map.entry(i, method.getParameters()[i]))
                .toList();

        this.pattern = loadPattern();
        this.headerParams = loadHeaderParams();
        this.pathParams = loadPathParams();
        this.clientOSIndex = getClientOSIndex();
        this.requestInfoIndex = getRequestInfoIndex();
        this.objectsByIndex = loadObjectsIndexes();
    }

    private Pattern loadPattern() {
        var regex = path.contains("{") && path.contains("}")
            ? path.replaceAll("\\{([^/]+)}", "([^/]+)")
            : path;

        return Pattern.compile("^" + regex + "$");
    }

    private Map<Integer, String> loadHeaderParams() {
        return dataParameters.stream()
            .filter(e -> e.getValue().isAnnotationPresent(HeaderParam.class))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAnnotation(HeaderParam.class).value()));
    }

    private Map<Integer, Map.Entry<String, Class<?>>> loadPathParams() {
        var matcher = Pattern.compile("\\{([^/]+)}").matcher(path);
        var paramsName = new ArrayList<String>();
        var pathParams = dataParameters.stream()
            .filter(p -> p.getValue().isAnnotationPresent(PathParam.class))
            .toList();

        while (matcher.find()) {
            var paramName = matcher.group(1);
            if (paramsName.contains(paramName)) {
                throw new IllegalStateException(
                    "O método " + this.method.getDeclaringClass().getName() + ":" +  this. method.getName() + " possui o path com parâmetros com o mesmo nome: " + paramName);
            }

            paramsName.add(paramName);
        }

        if (pathParams.size() != paramsName.size()) {
            var msg = String.format(
                "O path %s do método %s possui %d parâmetros, mas o método possui %d",
                path,
                this.method.getDeclaringClass().getName() + ":" +  this. method.getName(),
                paramsName.size(),
                pathParams.size());

            throw new IllegalStateException(msg);
        }

        var namesPathParam = pathParams.stream()
            .map(p -> p.getValue().getAnnotation(PathParam.class).value())
            .toList();

        var pathParamEqualsNamesPath = namesPathParam.stream()
            .allMatch(e -> Collections.frequency(namesPathParam, e) == Collections.frequency(paramsName, e));

        if (!pathParamEqualsNamesPath) {
            var msg = String.format(
                "Os nomes de variáveis fornecidos no @Path não conferem com os nomes do @PathParam no método: " + this.method.getDeclaringClass().getName() + ":" +  this. method.getName());

            throw new IllegalStateException(msg);
        }

        return pathParams.stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                p -> Map.entry(p.getValue().getAnnotation(PathParam.class).value(), p.getValue().getType()),
                (a, _) -> a,
                TreeMap::new
            ));
    }

    private Integer getClientOSIndex() {
        var clientsOS = dataParameters.stream()
            .filter(p -> p.getValue().isAnnotationPresent(ClientOS.class))
            .toList();

        if (clientsOS.isEmpty()) {
            return null;
        }

        if (clientsOS.size() > 1) {
            throw new IllegalStateException("O método " + this.method.getDeclaringClass().getName() + ":" +  this. method.getName() +  " só pode ter um ClientOS");
        }

        return clientsOS.getFirst().getKey();
    }

    private Integer getRequestInfoIndex() {
        var requestsInfo = dataParameters.stream()
            .filter(p -> p.getValue().getType().getName().equals(RequestInfo.class.getName()))
            .toList();

        if (requestsInfo.isEmpty()) {
            return null;
        }

        if (requestsInfo.size() > 1) {
            throw new IllegalStateException("O método " + this.method.getDeclaringClass().getName() + ":" +  this. method.getName() +  " só pode ter um RequestInfo");
        }

        return requestsInfo.getFirst().getKey();
    }

    private Map<Integer, Class<?>> loadObjectsIndexes() {
        return dataParameters.stream()
            .filter(p -> p.getValue().getAnnotations().length == 0)
            .filter(p -> !p.getKey().equals(this.requestInfoIndex))
            .map(p -> Map.entry(p.getKey(), p.getValue().getType()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, TreeMap::new));
    }

    public String nameMethod() {
        return this.method.getDeclaringClass().getName() + ":" + this.method.getName();
    }

    public boolean isMethod(String method, String requestURI) {
        return this.httpMethod.equalsIgnoreCase(method) && pattern.matcher(requestURI).matches();
    }

}
