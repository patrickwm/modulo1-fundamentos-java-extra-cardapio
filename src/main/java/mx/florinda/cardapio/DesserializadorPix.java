package mx.florinda.cardapio;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class DesserializadorPix {

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("pix.ser");

        ObjectInputStream ois = new ObjectInputStream(fis);
        Pix pix = (Pix) ois.readObject();

        System.out.println(pix);
        System.out.println(pix.getChaveDestino());
    }
}
