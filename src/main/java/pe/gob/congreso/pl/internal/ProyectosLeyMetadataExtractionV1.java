package pe.gob.congreso.pl.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

import static pe.gob.congreso.pl.Constants.BASE_URL_V1;

public class ProyectosLeyMetadataExtractionV1 {

  static class ProyectoLeyMetadataExtraction
      implements Function<ProyectosLey.ProyectoLey, ProyectosLeyMetadata.ProyectoLeyMetadata> {

    @Override
    public ProyectosLeyMetadata.ProyectoLeyMetadata apply(ProyectosLey.ProyectoLey pl) {
      try {
        var doc = Jsoup.connect(BASE_URL_V1 + pl.url()).get();
        var body = doc.body();
        var inputs = body.select("input");
        for (var i : inputs) {
          System.out.println(i);
        }

        var autores = autores(extractInputValue(inputs, "NomCongre"));
        var adherentes = autores(extractInputValue(inputs, "Adherentes"));

        var autor = autores.stream().findFirst();
        autores.remove(0);
        return new ProyectosLeyMetadata.ProyectoLeyMetadata(
            pl.periodo(),
            Integer.parseInt(Objects.requireNonNull(inputs.select("input[name=CodIni]").first()).attr("value")),
            extractInputValue(inputs, "CodIni_web"),
            extractInputValue(inputs, "DesLegis").orElse(""),
            LocalDate.parse(extractInputValue(inputs, "FecPres").get(),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")),
            extractInputValue(inputs, "DesPropo").orElse(""),
            extractInputValue(inputs, "TitIni").orElse(""),
            extractInputValue(inputs, "SumIni"),
            extractInputValue(inputs, "DesGrupParla"),
            extractInputValue(inputs, "CodUltEsta").orElse(""),

            autor, //firmantes.get(1),
            new HashSet<>(autores), //firmantes.get(2),
            new HashSet<>(adherentes), //firmantes.get(3),

            null, //seguimientos,
            null //seguimientos.stream().map(ProyectosLeyMetadata.Seguimiento::comision)
            //    .filter(Optional::isPresent)
             //   .map(Optional::get)
             //   .collect(Collectors.toSet())
        );
      } catch (Exception e) {
        throw new RuntimeException("Error", e);
      }
    }

    private List<ProyectosLeyMetadata.Congresista> autores(Optional<String> nomCongre) {
      var c = new ArrayList<ProyectosLeyMetadata.Congresista>();
      if (nomCongre.isPresent()) {
        for (var nombres : nomCongre.get().split(",")) {
          c.add(new ProyectosLeyMetadata.Congresista(
              nombres.replace("  ", ", "),
              Optional.empty(), Optional.empty(), Optional.empty()
          ));
        }

      }
      return c;
    }

    private Optional<String> extractInputValue(Elements inputs, String name) {
      return Optional.ofNullable(inputs.select("input[name=%s]".formatted(name)).first()).map(e -> e.attr("value"));
    }

    public static void main(String[] args) {
      var meta = new ProyectoLeyMetadataExtraction().apply(
          new ProyectosLey.ProyectoLey(Periodo._2016_2021,
              6,
              Optional.empty(),
              LocalDate.now(),
              "", "",
              //"/Sicr/TraDocEstProc/CLProLey1995.nsf/4ddaa53c5fb314620525800b007fc25d/cc8efeaa7d86e56105256ce1006a5dc2?OpenDocument"));
      "/Sicr/TraDocEstProc/CLProLey2016.nsf/641842f7e5d631bd052578e20058a231/7991e0c30e23ec630525871e0059e2b4?OpenDocument"));
      System.out.println(meta);
    }
  }
}
