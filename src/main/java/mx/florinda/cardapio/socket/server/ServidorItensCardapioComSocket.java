package mx.florinda.cardapio.socket.server;

import mx.florinda.cardapio.rest.RouteDefinition;
import mx.florinda.cardapio.rest.RouteRegistry;
import mx.florinda.cardapio.rest.annotatios.ErrorMapping;

import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mx.florinda.cardapio.Params.CRLF;

public class ServidorItensCardapioComSocket {
    private static final Logger logger = Logger.getLogger(ServidorItensCardapioComSocket.class.getName());

    public static void main(String[] args) throws Exception {
        RouteRegistry.initialize();

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
                var routeDefinitionOpt = searchExecutionMethod(method, requestURI);
                if (routeDefinitionOpt.isEmpty()) {
                    logger.warning("URI não encontrada: " + requestURI);
                    var responseLine = "HTTP/1.1 404 Not Found" + CRLF;
                    clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));

                    return;
                }

                var routeDefinition = routeDefinitionOpt.get();

                try {
                    var arguments = routeDefinition.getArguments(method, requestURI, headersParams, clientOS, requestChunks);
                    routeDefinition.executeMethod(arguments);
                } catch (InvocationTargetException ex) {
                    logger.log(Level.WARNING, ex, () -> "Erro ao tratar " + method + " " + requestURI);

                    var httpResponseLine = "HTTP/1.1 ";
                    var responseCodeOpt = routeDefinition.getResponseCode();

                    if (responseCodeOpt.isPresent()) {
                        var responseCode = responseCodeOpt.get();

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

    private static Optional<RouteDefinition> searchExecutionMethod(String method, String requestURI) {
        var methods = RouteRegistry.searchExecutionMethod(method, requestURI);

        if (methods.size() > 1) {
            var routesPath = methods.stream()
                .filter(m -> m.isPath(requestURI))
                .toList();

            if (routesPath.size() != 1) {
                var messageMethods = methods.stream()
                    .map(RouteDefinition::nameMethod)
                    .collect(Collectors.joining(CRLF));

                throw new IllegalStateException(
                    "Existe mais de um método com " + method + " e uri " + requestURI + CRLF + messageMethods);
            }

            return Optional.of(routesPath.getFirst());
        }

        return methods
                .stream()
                .findFirst();
    }
}
