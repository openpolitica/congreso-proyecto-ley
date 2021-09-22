package pe.gob.congreso.pl;

import java.io.IOException;
import java.util.Arrays;

public class Export {
  public static void main(String[] args) {
    Arrays.stream(Periodo.values()).parallel()
        .forEach(periodo -> {
          try {
            periodo.exportDbFromJson();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

//    try {
//      Periodo._1995_2000.exportDbFromJson();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }
}
