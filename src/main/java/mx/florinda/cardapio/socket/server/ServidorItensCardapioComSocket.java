package mx.florinda.cardapio.socket.server;

import com.google.gson.Gson;
import mx.florinda.cardapio.socket.server.rest.ClientOS;
import mx.florinda.cardapio.socket.server.rest.HeaderParam;
import mx.florinda.cardapio.socket.server.rest.Path;
import mx.florinda.cardapio.socket.server.rest.Rest;
import org.reflections.Reflections;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
                        .mapToObj(i -> new Pair<>(i, executionMethod.getParameters()[i]))
                        .toList();

                var allParamsWithOrder = loadValuesParametersHeadersByIndex(paramsWithIndex, headersParams);
                loadClientOSParameter(paramsWithIndex, allParamsWithOrder, clientOS);
                loadRequestInfoParameter(paramsWithIndex, allParamsWithOrder, method, requestURI);
                loadBodyParameter(paramsWithIndex, allParamsWithOrder, requestChunks);
                setOtherParametersNull(paramsWithIndex, allParamsWithOrder);

                try {
                    var restClass = executionMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
                    executionMethod.invoke(restClass, allParamsWithOrder.values().toArray());
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
                .filter(m -> Arrays.asList(m.getAnnotation(Path.class).value()).contains(requestURI))
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

    private static TreeMap<Integer, Object> loadValuesParametersHeadersByIndex(
            List<Pair<Integer, Parameter>> paramsWithIndex,
            Map<String, String> headersParams) {

        var valueAnnotationHeaderParam = (Function<Pair<Integer, Parameter>, String>)
                p -> headersParams.get(p.second().getAnnotation(HeaderParam.class).value());

        return paramsWithIndex
            .stream()
            .filter(p -> p.second().isAnnotationPresent(HeaderParam.class))
            .filter(p -> valueAnnotationHeaderParam.apply(p) != null)
            .map(p -> new Pair<>(p.first(), valueAnnotationHeaderParam.apply(p)))
            .collect(Collectors.toMap(Pair::first, Pair::second, (a, _) -> a, TreeMap::new));
    }

    private static void loadClientOSParameter(List<Pair<Integer, Parameter>> paramsWithIndex,
                                              TreeMap<Integer, Object> allParamsWithOrder, OutputStream clientOS) {
        paramsWithIndex
                .stream()
                .filter(p -> p.second().isAnnotationPresent(ClientOS.class))
                .map(p -> new Pair<>(p.first(), clientOS))
                .findFirst()
                .ifPresent(e -> allParamsWithOrder.put(e.first(), e.second()));
    }

    private static void loadRequestInfoParameter(List<Pair<Integer, Parameter>> paramsWithIndex,
                                                 TreeMap<Integer, Object> allParamsWithOrder, String method,
                                                 String requestURI) {
        paramsWithIndex
                .stream()
                .filter(p -> p.second().getType().getName().equals(RequestInfo.class.getName()))
                .map(p -> new Pair<>(p.first(), new RequestInfo(method, requestURI)))
                .findFirst()
                .ifPresent(e -> allParamsWithOrder.put(e.first(), e.second()));
    }

    private static void loadBodyParameter(List<Pair<Integer, Parameter>> paramsWithIndex,
                                          TreeMap<Integer, Object> allParamsWithOrder, String[] requestChunks) {
        paramsWithIndex
                .stream()
                .filter(p -> !allParamsWithOrder.containsKey(p.first()))
                .filter(p -> p.second().getAnnotations().length == 0)
                .findFirst()
                .map(p -> {
                    var value = requestChunks.length > 1
                            ? transformBody(requestChunks[1], p.second())
                            : null;

                    return allParamsWithOrder.put(p.first(), value);
                });
    }

    private static Object transformBody(String body, Parameter p) {
        var gson = new Gson();
        return gson.fromJson(body, p.getType());
    }

    private static void setOtherParametersNull(List<Pair<Integer, Parameter>> paramsWithIndex,
                                               TreeMap<Integer, Object> allParamsWithOrder) {
        paramsWithIndex
                .stream()
                .filter(p -> !allParamsWithOrder.containsKey(p.first()))
                .forEach(p -> allParamsWithOrder.put(p.first(), null));
    }

    private record Pair<A, B>(A first, B second) {
    }

}
