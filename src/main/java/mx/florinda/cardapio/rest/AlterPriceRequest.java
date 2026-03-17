package mx.florinda.cardapio.rest;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public record AlterPriceRequest(@SerializedName("preco") BigDecimal price) {
}
