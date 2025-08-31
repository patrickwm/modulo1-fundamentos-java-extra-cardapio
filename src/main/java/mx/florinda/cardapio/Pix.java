package mx.florinda.cardapio;

import java.io.Serializable;
import java.math.BigDecimal;

public class Pix implements Serializable {

    private Long id;
    private BigDecimal valor;
    private String chaveDestino;

    public Pix(Long id, BigDecimal valor, String chaveDestino) {
        this.id = id;
        this.valor = valor;
        this.chaveDestino = chaveDestino;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getChaveDestino() {
        return chaveDestino;
    }

    public void setChaveDestino(String chaveDestino) {
        this.chaveDestino = chaveDestino;
    }
}
