package mx.florinda.cardapio.rest;

import com.google.gson.Gson;
import mx.florinda.cardapio.ItemCardapio;
import mx.florinda.cardapio.application.cardapio.AlterPriceItemCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.CountItemsCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.CreateItemCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.DeleteItemCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.GetItemCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.ItemCardapioFileUseCase;
import mx.florinda.cardapio.application.cardapio.ListItemCardapioUseCase;
import mx.florinda.cardapio.application.cardapio.PageRootUseCase;
import mx.florinda.cardapio.application.exception.NotFoundException;
import mx.florinda.cardapio.rest.annotatios.methods.Delete;
import mx.florinda.cardapio.rest.annotatios.ErrorMapping;
import mx.florinda.cardapio.rest.annotatios.methods.Patch;
import mx.florinda.cardapio.rest.annotatios.params.PathParam;
import mx.florinda.cardapio.rest.annotatios.methods.ResponseCode;
import mx.florinda.cardapio.socket.server.RequestInfo;
import mx.florinda.cardapio.rest.annotatios.params.ClientOS;
import mx.florinda.cardapio.rest.annotatios.methods.Get;
import mx.florinda.cardapio.rest.annotatios.params.HeaderParam;
import mx.florinda.cardapio.rest.annotatios.methods.Path;
import mx.florinda.cardapio.rest.annotatios.methods.Post;
import mx.florinda.cardapio.rest.annotatios.Rest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static mx.florinda.cardapio.Params.CRLF;

@Rest
public class CardapioSocketRest {
    private static final Logger logger = Logger.getLogger(CardapioSocketRest.class.getName());

    @Get
    @Path("/itens-cardapio")
    public void listAllItensCardapio(@HeaderParam("Accept") String accept, @ClientOS OutputStream clientOS)
            throws IOException {
        logger.fine("Chamou listagem de itens de cardápio");
        var useCase = new ListItemCardapioUseCase();
        var dto = useCase.execute(accept);
        var responseLine = "HTTP/1.1 " + HttpStatus.OK + CRLF;
        var responseContentType = "Content-type: %s; charset=UTF-8%s%s".formatted(dto.mediaType(), CRLF, CRLF);

        writeResponse(clientOS, responseLine, responseContentType, dto.body());
    }

    @Get
    @Path("/itens-cardapio/{id}")
    @ResponseCode(
        fail = {
            @ErrorMapping(exception = NotFoundException.class, status = HttpStatus.NOT_FOUND)
        }
    )
    public void getItemCardapio(@HeaderParam("Accept") String accept, @PathParam("id") Long id, @ClientOS OutputStream clientOS)
            throws IOException {
        logger.fine("Chamou listagem de itens de cardápio");
        var useCase = new GetItemCardapioUseCase();
        var dto = useCase.execute(new GetItemCardapioUseCase.ItemCardapioSearch(accept, id));
        var responseLine = "HTTP/1.1 " + HttpStatus.OK + CRLF;
        var responseContentType = "Content-type: %s; charset=UTF-8%s%s".formatted(dto.mediaType(), CRLF, CRLF);

        writeResponse(clientOS, responseLine, responseContentType, dto.body());
    }

    @Delete
    @Path("/itens-cardapio/{id}")
    @ResponseCode(
        fail = {
            @ErrorMapping(exception = NotFoundException.class, status = HttpStatus.NOT_FOUND)
        }
    )
    public void deleteItemCardapio(@PathParam("id") Long id, @ClientOS OutputStream clientOS)
            throws IOException {
        logger.fine("Chamou listagem de itens de cardápio");
        var useCase = new DeleteItemCardapioUseCase();
        useCase.execute(id);

        var responseLine = "HTTP/1.1 " + HttpStatus.NO_CONTENT + CRLF;
        var responseContentType = "Content-type: application/json; charset=UTF-8"  + CRLF +  CRLF;
        writeResponse(clientOS, responseLine, responseContentType, (String) null);
    }

    @Get
    @Path("/itens-cardapio/total")
    public void countItensCardapio(@ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou total de itens de cardápio");
        var totalItens = new CountItemsCardapioUseCase().execute();
        var responseLine = "HTTP/1.1 " + HttpStatus.OK + CRLF;
        var responseContentType = "Content-type: application/json; charset=UTF-8%s%s" + CRLF + CRLF;
        var totalBody = new Gson().toJson(totalItens);

        writeResponse(clientOS, responseLine, responseContentType, totalBody);
    }

    @Get
    @Path("/itensCardapio.json")
    public void fileItensCardapio(RequestInfo requestInfo, @ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou arquivo itensCardapio.json");

        var dataFile = new ItemCardapioFileUseCase().execute(requestInfo.uri());
        var responseLine = "HTTP/1.1 " + HttpStatus.OK + CRLF;
        var responseContentType = "Content-type: application/json; charset=UTF-8%s%s" + CRLF + CRLF;

        writeResponse(clientOS, responseLine, responseContentType, dataFile);
    }

    @Get
    @Path({"/", "/en"})
    public void index(RequestInfo requestInfo, @ClientOS OutputStream clientOS) throws IOException {
        logger.fine("Chamou página raiz");

        var html = new PageRootUseCase().execute(requestInfo.uri());
        var responseLine = "HTTP/1.1 " + HttpStatus.OK + CRLF;
        var responseContentType = "Content-type: text/html; charset=UTF-8%s%s" + CRLF + CRLF;

        writeResponse(clientOS, responseLine, responseContentType, html);
    }

    @Post
    @Path("/itens-cardapio")
    public void createItemCardapio(@ClientOS OutputStream clientOS, ItemCardapio itemCardapio) throws IOException {
        logger.fine("Chamou adição de itens de cardápio");

        new CreateItemCardapioUseCase().execute(itemCardapio);
        var responseLine = "HTTP/1.1 " + HttpStatus.CREATED + CRLF;
        writeResponse(clientOS, responseLine, null, (String) null);
    }

    @Patch
    @Path("/itens-cardapio/{id}/price")
    @ResponseCode(
        fail = {
            @ErrorMapping(exception = NotFoundException.class, status = HttpStatus.NOT_FOUND)
        }
    )
    public void alterPriceItemCardapio(@ClientOS OutputStream clientOS, @PathParam("id") Long id, AlterPriceRequest alterPriceRequest) throws IOException {
        logger.fine("Chamou adição de itens de cardápio");

        var useCase = new AlterPriceItemCardapioUseCase();
        useCase.execute(new AlterPriceItemCardapioUseCase.AlterPrice(id, alterPriceRequest.price()));

        var responseLine = "HTTP/1.1 " + HttpStatus.NO_CONTENT + CRLF;
        writeResponse(clientOS, responseLine, null, (String) null);
    }

    private void writeResponse(OutputStream clientOS, String responseLine, String responseContentType, String body)
            throws IOException {
        var valueBody = body != null ? body.getBytes(StandardCharsets.UTF_8) : null;
        writeResponse(clientOS, responseLine, responseContentType, valueBody);
    }

    private void writeResponse(OutputStream clientOS, String responseLine, String responseContentType, byte[] body)
            throws IOException {
        clientOS.write(responseLine.getBytes(StandardCharsets.UTF_8));

        if (responseContentType != null) {
            clientOS.write(responseContentType.getBytes(StandardCharsets.UTF_8));
        }

        if (body != null) {
            clientOS.write(body);
        }

        clientOS.flush();
    }
}

