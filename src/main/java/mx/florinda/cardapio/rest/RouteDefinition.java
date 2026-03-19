package mx.florinda.cardapio.rest;

import com.google.gson.Gson;
import mx.florinda.cardapio.rest.annotatios.methods.ResponseCode;
import mx.florinda.cardapio.rest.annotatios.params.ClientOS;
import mx.florinda.cardapio.rest.annotatios.params.HeaderParam;
import mx.florinda.cardapio.rest.annotatios.params.PathParam;
import mx.florinda.cardapio.socket.server.RequestInfo;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
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
    private Map<Integer, PathParamData> pathParamsBySeqPath;
    private Integer clientOSIndex;
    private Integer requestInfoIndex;
    private Map<Integer, Class<?>> objectsByIndex;
    private ResponseCode responseCode;
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
        this.pathParamsBySeqPath = loadPathParams();
        this.clientOSIndex = getClientOSIndex();
        this.requestInfoIndex = getRequestInfoIndex();
        this.objectsByIndex = loadObjectsIndexes();
        this.responseCode = method.getAnnotation(ResponseCode.class);
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

    private Map<Integer, PathParamData> loadPathParams() {
        var matcher = Pattern.compile("\\{([^/]+)}").matcher(path);
        var paramsName = new ArrayList<String>();

        while (matcher.find()) {
            var paramName = matcher.group(1);
            if (paramsName.contains(paramName)) {
                throw new IllegalStateException(
                    "O método " + this.method.getDeclaringClass().getName() + ":" +  this. method.getName() + " possui o path com parâmetros com o mesmo nome: " + paramName);
            }

            paramsName.add(paramName);
        }

        var pathParams = dataParameters.stream()
            .filter(p -> p.getValue().isAnnotationPresent(PathParam.class))
            .toList();

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

        var pathParamsByName = pathParams.stream()
            .collect(Collectors.toMap(
                e -> e.getValue().getAnnotation(PathParam.class).value(),
                v -> new PathParamData(v.getKey(), v.getValue().getAnnotation(PathParam.class).value(), v.getValue().getType())));

        return IntStream.range(0, paramsName.size())
            .mapToObj(i -> Map.entry(i, pathParamsByName.get(paramsName.get(i))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public boolean isPath(String requestURI) {
        return this.path.equals(requestURI);
    }

    public Optional<ResponseCode> getResponseCode() {
        return Optional.ofNullable(responseCode);
    }

    public void executeMethod(Object[] arguments) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var restClass = this.method.getDeclaringClass().getDeclaredConstructor().newInstance();
        this.method.invoke(restClass, arguments);
    }

    public Object[] getArguments(String method, String requestURI, Map<String, String> headers, OutputStream clientOS, String[] requestChunks) {
        var argumentsWithIndex = new ArrayList<IndexValueArgument>();

        loadHeaderParams(argumentsWithIndex, headers);
        loadIndexes(argumentsWithIndex, clientOS, method, requestURI);
        loadValuesPathParam(argumentsWithIndex, requestURI);
        loadObjectsIndexes(argumentsWithIndex, requestChunks);
        loadNullsOtherIndexes(argumentsWithIndex);

        return argumentsWithIndex
            .stream()
            .sorted(Comparator.comparingInt(IndexValueArgument::index))
            .map(IndexValueArgument::value)
            .toArray();
    }

    private void loadHeaderParams(List<IndexValueArgument> argumentsWithIndex, Map<String, String> headers) {
        headerParams.forEach((k, v) -> argumentsWithIndex.add(new IndexValueArgument(k, headers.get(v))));
    }

    private void loadIndexes(List<IndexValueArgument> argumentsWithIndex, OutputStream clientOS, String method, String requestURI) {
        Optional.ofNullable(clientOSIndex)
            .ifPresent(i -> argumentsWithIndex.add(new IndexValueArgument(i, clientOS)));

        Optional.ofNullable(requestInfoIndex)
            .ifPresent(i -> argumentsWithIndex.add(i, new IndexValueArgument(i, new RequestInfo(method, requestURI))));
    }

    private void loadValuesPathParam(List<IndexValueArgument> argumentsWithIndex, String requestURI) {
        if (pathParamsBySeqPath.isEmpty()) {
            return;
        }

        var matcherPath = pattern.matcher(requestURI);
        var values = new ArrayList<String>();

        while (matcherPath.find()) {
            values.add(matcherPath.group(1));
        }

        if (pathParamsBySeqPath.size() != values.size()) {
            var namesPathParam = pathParamsBySeqPath.values()
                .stream()
                .map(PathParamData::name)
                .collect(Collectors.joining(", "));

            throw new IllegalStateException(
                String.format(
                    "Erro ao mepear campos @PathParam. PathParams encontrados: %s | Valores encontrados: %s",
                    String.join(", ", namesPathParam),
                    String.join(", ", values)));
        }

        var pathParamByPathParamData = IntStream.range(0, pathParamsBySeqPath.size())
            .mapToObj(i -> Map.entry(pathParamsBySeqPath.get(i), values.get(i)))
            .collect(Collectors.toMap(Map.Entry::getKey, v -> Map.entry(v.getKey(), v.getValue()), (a, _) -> a, TreeMap::new));

        pathParamsBySeqPath.forEach((_, pathParamData) -> {
            var entry = pathParamByPathParamData.get(pathParamData);

            argumentsWithIndex.add(new IndexValueArgument(pathParamData.index(), convert(entry.getValue(), pathParamData.type())));
        });
    }

    private void loadObjectsIndexes(List<IndexValueArgument> argumentsWithIndex, String[] requestChunks) {
        objectsByIndex.forEach((index, clazz) -> {
            var value = requestChunks.length > 1
                ? transformBody(requestChunks[1], clazz)
                : null;

            argumentsWithIndex.add(new IndexValueArgument(index, value));
        });
    }

    private void loadNullsOtherIndexes(List<IndexValueArgument> argumentsWithIndex) {
        var filledIndexes = argumentsWithIndex.stream()
            .map(IndexValueArgument::index)
            .collect(Collectors.toSet());

        dataParameters
            .stream()
            .filter(p -> !filledIndexes.contains(p.getKey()))
            .forEach(p -> argumentsWithIndex.add(new IndexValueArgument(p.getKey(), null)));
    }

    private Object convert(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        }

        if (type == Long.class || type == long.class) {
            return Long.valueOf(value);
        }

        if (type == Integer.class || type == int.class) {
            return Integer.valueOf(value);
        }

        if (type == BigDecimal.class) {
            return new BigDecimal(value);
        }

        return value;
    }

    private static Object transformBody(String body, Class<?> clazz) {
        var gson = new Gson();
        return gson.fromJson(body, clazz);
    }

    private record PathParamData(int index, String name, Class<?> type) implements Comparable<PathParamData> {
        @Override
        public int compareTo(PathParamData o) {
            return Integer.compare(this.index, o.index);
        }
    }

    private record IndexValueArgument(int index, Object value) {
    }

}
