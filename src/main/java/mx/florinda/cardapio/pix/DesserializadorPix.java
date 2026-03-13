package mx.florinda.cardapio.pix;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class DesserializadorPix {

    public static void main(String[] args) throws Exception {
        var fis = new FileInputStream("pix.ser");

        var ois = new ObjectInputStream(fis);
        var pix = (Pix) ois.readObject();

        System.out.println(pix);
        System.out.println(pix.getChaveDestino());
    }
}
