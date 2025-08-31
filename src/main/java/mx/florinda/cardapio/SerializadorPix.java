package mx.florinda.cardapio;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

public class SerializadorPix {

    public static void main(String[] args) throws Exception {

        var pix = new Pix(1L, new BigDecimal("10.99"), "alexandre.aquiles@gmail.com");
        FileOutputStream fos = new FileOutputStream("pix.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(pix);

    }
}
