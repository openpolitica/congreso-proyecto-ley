package pe.gob.congreso.pl;

import java.io.IOException;
import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    //Periodo._1995_2000.save();
    //Periodo._2000_2001.save();
    //Periodo._2001_2006.save();
    //Periodo._2006_2011.save();
    //Periodo._2011_2016.save();
    //Periodo._2016_2021.save();
    //Periodo._2021_2026.save();

    //Arrays.stream(Periodo.values()).parallel()
    //    .forEach(periodo -> {
    //      try {
    //        periodo.save();
    //      } catch (IOException e) {
    //        e.printStackTrace();
    //      }
    //    });

    Arrays.stream(Periodo.values()).parallel()
        .forEach(periodo -> {
          try {
            periodo.saveFromJson();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    //try {
    //  Periodo._2021_2026.saveFromJson();
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}
  }
}
