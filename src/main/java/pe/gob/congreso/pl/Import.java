package pe.gob.congreso.pl;

import java.io.IOException;
import java.util.Arrays;

public class Import {
  public static void main(String[] args) {
    Arrays.stream(Periodo.values()).parallel()
        .forEach(periodo -> {
          try {
            periodo.importJson();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

//    try {
//      Periodo._1995_2000.importJson();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }
}
