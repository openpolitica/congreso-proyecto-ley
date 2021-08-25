package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

import static java.util.stream.Collectors.toList;
import static pe.gob.congreso.pl.Constants.BASE_URL_V1;

public class ProyectosLeyMetadataExtractionV1 {

  static class ProyectoLeyMetadataExtraction
      implements Function<ProyectosLey.ProyectoLey, ProyectosLeyMetadata.ProyectoLeyMetadata> {
    static final Pattern datePattern = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");

    @Override
    public ProyectosLeyMetadata.ProyectoLeyMetadata apply(ProyectosLey.ProyectoLey pl) {
      try {
        var doc = Jsoup.connect(BASE_URL_V1 + pl.url()).get();
        var body = doc.body();
        var inputs = body.select("input");
        //rm
        for (var i : inputs) {
          System.out.println(i);
        }

        var seg =
        body.select("tr").stream().filter(e -> e.text().startsWith("Seguimiento:"))
            .findAny()
            .map(Element::text)
            .orElse(body.select("td").stream().filter(e -> e.select("b").text().equals("Envío a Comisión:"))
                .map(e -> e.select("font[size=3]").text()).findAny().orElse(""));

        var seguimientos = new LinkedHashSet<ProyectosLeyMetadata.Seguimiento>();
        if (!seg.isBlank()) {
          var matcher = datePattern.matcher(seg);
          var textos = Arrays.stream(seg.split(datePattern.pattern()))
              .map(String::trim)
              .filter(s -> !s.isBlank())
              .collect(toList());
          for (String texto : textos) {
            if (matcher.find()) {
              var fecha = matcher.group();
              seguimientos.add(new ProyectosLeyMetadata.Seguimiento(
                  LocalDate.parse(fecha, DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                  texto,
                  Optional.empty(),
                  Optional.empty()
              ));
            }
          }
        }

        var prefix = "En comisión";
        var comisiones = new LinkedHashSet<String>();
        for (var s : seguimientos) {
          if (s.detalle().startsWith(prefix)) {
            final var comision = s.detalle().substring(prefix.length() + 1).strip();
            if (comision.contains("-")) {
              var corregido = comision.substring(0, comision.indexOf("-"));
              comisiones.add(corregido);
            } else {
              comisiones.add(comision);
            }
          }
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

            seguimientos,
            comisiones
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
              "/Sicr/TraDocEstProc/CLProLey1995.nsf/4ddaa53c5fb314620525800b007fc25d/cc8efeaa7d86e56105256ce1006a5dc2?OpenDocument"));
      //"/Sicr/TraDocEstProc/CLProLey2016.nsf/641842f7e5d631bd052578e20058a231/175cc98efc894940052587120055dbf9?OpenDocument"));
      System.out.println(meta);
    }
  }
}
