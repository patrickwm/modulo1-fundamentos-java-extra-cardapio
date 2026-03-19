package mx.florinda.cardapio.socket.server;

import com.google.gson.Gson;
import mx.florinda.cardapio.rest.annotatios.params.ClientOS;
import mx.florinda.cardapio.rest.annotatios.ErrorMapping;
import mx.florinda.cardapio.rest.annotatios.params.HeaderParam;
import mx.florinda.cardapio.rest.annotatios.methods.Path;
import mx.florinda.cardapio.rest.annotatios.params.PathParam;
import mx.florinda.cardapio.rest.annotatios.methods.ResponseCode;
import mx.florinda.cardapio.rest.annotatios.Rest;
import org.reflections.Reflections;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mx.florinda.cardapio.Params.CRLF;

public class ServidorItensCardapioComSocket {
    private static final Logger logger = Logger.getLogger(ServidorItensCardapioComSocket.class.getName());

    public static void main(String[] args) throws Exception {
        try (var executor = Executors.newFixedThreadPool(50);
             var serverSocket = new ServerSocket(8000)) {
            logger.info("Subiu servidor!");

            while (true) {
                var clientSocket = serverSocket.accept();
                executor.execute(() -> processRequest(clientSocket));
            }
        }
    }

    private static void processRequest(Socket clientSocket) {
        try (clientSocket) {
            var clientIS = clientSocket.getInputStream();

            var requestBuilder = new StringBuilder();
            int data;
            do {
                data = clientIS.read();
                requestBuilder.append((char) data);
            } while (clientIS.available() > 0);

            var request = requestBuilder.toString();
            logger.finest(request);
            logger.fine("\n\nChegou um novo request");

            var requestChunks = request.split(CRLF + CRLF);
            var requestLineAndHeaders = requestChunks[0];
            var requestLineAndHeadersChunks = requestLineAndHeaders.split(CRLF);
            var requestLine = requestLineAndHeadersChunks[0];
            var requestLineChunks = requestLine.split(" ");
            var method = requestLineChunks[0];
            var requestURI = requestLineChunks[1];
            var httpVersion = requestLineChunks[2];
            var headersParams = getHeaders(requestLineAndHeadersChunks);

            logger.finer(() -> "Method: " + method);
            logger.finer(() -> "Request URI: " + requestURI);
            logger.finer(() -> "HTTP Version: " + httpVersion);

            Thread.sleep(250);

            try (var clientOS = clientSocket.getOutputStream()) {
                var executionMethodOpt = searchExecutionMethod(method, requestURI);
                if (executionMethodOpt.isEmpty()) {
                    logger.warning("URI não encontrada: " + requestURI);
                    var responseLine = "HTTP/1.1 404 Not Found" + CRLF;
                    clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));

                    return;
                }

                var executionMethod = executionMethodOpt.get();

                var paramsWithIndex = IntStream.range(0, executionMethod.getParameterCount())
                        .mapToObj(i -> Map.entry(i, executionMethod.getParameters()[i]))
                        .toList();

                var allParamsWithOrder = loadValuesParametersHeadersByIndex(paramsWithIndex, headersParams);
                loadClientOSParameter(paramsWithIndex, allParamsWithOrder, clientOS);
                loadRequestInfoParameter(paramsWithIndex, allParamsWithOrder, method, requestURI);
                loadPathParamParameter(paramsWithIndex, allParamsWithOrder, executionMethod, method, requestURI);
                loadBodyParameter(paramsWithIndex, allParamsWithOrder, requestChunks);
                setOtherParametersNull(paramsWithIndex, allParamsWithOrder);

                try {
                    var restClass = executionMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
                    executionMethod.invoke(restClass, allParamsWithOrder.values().toArray());
                } catch (InvocationTargetException ex) {
                    logger.log(Level.WARNING, ex, () -> "Erro ao tratar " + method + " " + requestURI);

                    var httpResponseLine = "HTTP/1.1 ";

                    if (executionMethod.isAnnotationPresent(ResponseCode.class)) {
                        var responseCode = executionMethod.getAnnotation(ResponseCode.class);

                        var responseCodeByClass = Arrays.stream(responseCode.fail())
                                .collect(Collectors.toMap(ErrorMapping::exception, ErrorMapping::status));

                        if (responseCodeByClass.containsKey(ex.getCause().getClass())) {
                            var httpStatus = responseCodeByClass.get(ex.getCause().getClass());

                            var responseLine = httpResponseLine + httpStatus.getCode() + " " + httpStatus.getDescription() + CRLF;
                            clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));

                            return;
                        }
                    }

                    var responseLine = "HTTP/1.1 500 Internal Server Error" + CRLF;
                    clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, ex, () -> "Erro ao tratar " + method + " " + requestURI);

                    var responseLine = "HTTP/1.1 500 Internal Server Error" + CRLF;
                    clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Erro fatal no servidor", ex);
            throw new RuntimeException(ex);
        }
    }

    private static Map<String, String> getHeaders(String[] requestLineAndHeadersChunks) {
        return Arrays.stream(requestLineAndHeadersChunks)
                .skip(1)
                .filter(header -> header.contains(":"))
                .map(header -> header.split(":", 2))
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }

    private static Optional<Method> searchExecutionMethod(String method, String requestURI) {
        var reflection = new Reflections("mx.florinda.cardapio");

        var methods = reflection.getTypesAnnotatedWith(Rest.class)
                .stream()
                .flatMap(c -> Arrays.stream(c.getMethods()))
                .filter(m -> existsMethodInAnnotations(m, method))
                .filter(m -> m.isAnnotationPresent(Path.class))
                .filter(m -> pathEqualsRequestURI(m, requestURI))
                .toList();

        if (methods.size() > 1) {
            var messageMethods = methods.stream()
                    .map(m -> m.getDeclaringClass().getName() + " -> " + m.getName())
                    .collect(Collectors.joining(CRLF));

            throw new IllegalStateException(
                    "Existe mais de um método com " + method + "e uri" + requestURI + CRLF + messageMethods);
        }

        return methods
                .stream()
                .findFirst();
    }

    private static boolean existsMethodInAnnotations(Method m, String method) {
        return Arrays.stream(m.getAnnotations())
                .map(annotation -> annotation.annotationType().getSimpleName())
                .anyMatch(a -> a.equalsIgnoreCase(method));
    }

    private static boolean pathEqualsRequestURI(Method m, String requestURI) {
        var valuesPath = Arrays.asList(m.getAnnotation(Path.class).value());
        var containsPathParam = valuesPath.stream()
                .anyMatch(p -> p.contains("{") && p.contains("}"));

        if (!containsPathParam) {
            return valuesPath.contains(requestURI);
        }

        if (valuesPath.size() > 1) {
            throw new IllegalStateException("Métodos com variáveis {} não podem ter mais de um path");
        }

        return valuesPath.stream()
                .map(p -> p.replaceAll("\\{.*}", ".*"))
                .anyMatch(p -> Pattern.compile(p).matcher(requestURI).matches());
    }

    private static TreeMap<Integer, Object> loadValuesParametersHeadersByIndex(
            List<Map.Entry<Integer, Parameter>> paramsWithIndex,
            Map<String, String> headersParams) {

        var valueAnnotationHeaderParam = (Function<Map.Entry<Integer, Parameter>, String>)
                p -> headersParams.get(p.getValue().getAnnotation(HeaderParam.class).value());

        return paramsWithIndex
                .stream()
                .filter(p -> p.getValue().isAnnotationPresent(HeaderParam.class))
                .filter(p -> valueAnnotationHeaderParam.apply(p) != null)
                .map(p -> Map.entry(p.getKey(), valueAnnotationHeaderParam.apply(p)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, TreeMap::new));
    }

    private static void loadClientOSParameter(List<Map.Entry<Integer, Parameter>> paramsWithIndex,
                                              TreeMap<Integer, Object> allParamsWithOrder, OutputStream clientOS) {
        paramsWithIndex
                .stream()
                .filter(p -> p.getValue().isAnnotationPresent(ClientOS.class))
                .map(p -> Map.entry(p.getKey(), clientOS))
                .findFirst()
                .ifPresent(e -> allParamsWithOrder.put(e.getKey(), e.getValue()));
    }

    private static void loadRequestInfoParameter(List<Map.Entry<Integer, Parameter>> paramsWithIndex,
                                                 TreeMap<Integer, Object> allParamsWithOrder, String method,
                                                 String requestURI) {
        paramsWithIndex
                .stream()
                .filter(p -> p.getValue().getType().getName().equals(RequestInfo.class.getName()))
                .map(p -> Map.entry(p.getKey(), new RequestInfo(method, requestURI)))
                .findFirst()
                .ifPresent(e -> allParamsWithOrder.put(e.getKey(), e.getValue()));
    }

    private static void loadPathParamParameter(List<Map.Entry<Integer, Parameter>> paramsWithIndex,
                                               TreeMap<Integer, Object> allParamsWithOrder, Method m,
                                               String method, String requestURI) {

        var existsPathParam = Arrays.stream(m.getParameters())
                .anyMatch(p -> p.isAnnotationPresent(PathParam.class));

        if (!existsPathParam) {
            return;
        }

        var valuePath = Arrays.stream(m.getAnnotation(Path.class).value()).findFirst().get();
        var names = new ArrayList<String>();
        var matcher = Pattern.compile("\\{([^/]+)}").matcher(valuePath);

        while (matcher.find()) {
            names.add(matcher.group(1));
        }

        var regexPath = valuePath.replaceAll("\\{([^/]+)}", "([^/]+)");
        var matcherPath = Pattern.compile("^" + regexPath + "$").matcher(requestURI);
        var values = new ArrayList<String>();

        while (matcherPath.find()) {
            values.add(matcherPath.group(1));
        }

        if (names.size() != values.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Erro ao mepear campos @PathParam. PathParams encontrados: %s | Valores encontrados: %s",
                            String.join(", ", names),
                            String.join(", ", values)));
        }

        var pathParamByName = IntStream.range(0, names.size())
                .mapToObj(i -> Map.entry(names.get(i), values.get(i)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, TreeMap::new));

        paramsWithIndex
                .stream()
                .filter(p -> p.getValue().isAnnotationPresent(PathParam.class))
                .map(p -> Map.entry(p.getKey(),
                        convert(pathParamByName.get(p.getValue().getAnnotation(PathParam.class).value()),
                                p.getValue().getType())))
                .findFirst()
                .ifPresent(e -> allParamsWithOrder.put(e.getKey(), e.getValue()));
    }

    private static Object convert(String value, Class<?> type) {
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

    private static void loadBodyParameter(List<Map.Entry<Integer, Parameter>> paramsWithIndex,
                                          TreeMap<Integer, Object> allParamsWithOrder, String[] requestChunks) {
        paramsWithIndex
                .stream()
                .filter(p -> !allParamsWithOrder.containsKey(p.getKey()))
                .filter(p -> p.getValue().getAnnotations().length == 0)
                .findFirst()
                .map(p -> {
                    var value = requestChunks.length > 1
                            ? transformBody(requestChunks[1], p.getValue())
                            : null;

                    return allParamsWithOrder.put(p.getKey(), value);
                });
    }

    private static Object transformBody(String body, Parameter p) {
        var gson = new Gson();
        return gson.fromJson(body, p.getType());
    }

    private static void setOtherParametersNull(List<Map.Entry<Integer, Parameter>> paramsWithIndex,
                                               TreeMap<Integer, Object> allParamsWithOrder) {
        paramsWithIndex
                .stream()
                .filter(p -> !allParamsWithOrder.containsKey(p.getKey()))
                .forEach(p -> allParamsWithOrder.put(p.getKey(), null));
    }
}
