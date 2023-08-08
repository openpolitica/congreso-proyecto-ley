package op.congreso.pl;

import java.io.IOException;
import java.util.Arrays;

public class Import {
  public static void main(String[] args) {
    Arrays.stream(Periodo.values()).parallel()
//            .filter(periodo -> periodo.equals(Periodo._2021_2026))
        .forEach(periodo -> {
          try {
            periodo.importJson();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }
}
