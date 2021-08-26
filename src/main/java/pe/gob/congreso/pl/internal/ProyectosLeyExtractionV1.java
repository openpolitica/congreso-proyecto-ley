package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;

import static pe.gob.congreso.pl.Constants.BASE_URL_V1;

public class ProyectosLeyExtractionV1 implements Function<Periodo, ProyectosLey> {

  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyExtractionV1.class);

  @Override public ProyectosLey apply(Periodo periodo) {
    try {
      var pls = new ProyectosLey(periodo);

      var index = 1;

      var first = periodo.baseUrl() + index;
      LOG.info("Extracting PL list from {}", first);
      var proyectos = extractPaginaProyectos(periodo, first);
      pls.addAll(proyectos);
      while (proyectos.size() == periodo.batchSize()) {
        index = index + periodo.batchSize();
        var url = periodo.baseUrl() + index;
        LOG.info("Extracting PL list from {}", url);
        proyectos = extractPaginaProyectos(periodo, url);
        pls.addAll(proyectos);
      }
      LOG.info("{} PLs extracted", pls.proyectos().size());
      return pls;
    } catch (Exception e) {
      throw new RuntimeException("Error", e);
    }
  }

  private Set<ProyectosLey.ProyectoLey> extractPaginaProyectos(Periodo periodo, String url) throws IOException {
    var proyectos = new LinkedHashSet<ProyectosLey.ProyectoLey>();
    var doc = Jsoup.connect(url).get();
    var table = doc.body().select("table[cellpadding=2]").first();
    if (table == null) throw new IllegalStateException("table not found");
    try {
      var trs = table.select("tr[valign=top]");
      for (var tr : trs) {
        var tds = tr.select("td");
        var pl = new ProyectosLey.ProyectoLey(
            periodo,
            extractNumero(tds.get(0)),
            Optional.ofNullable(extractFecha(tds.get(1))),
            extractFecha(tds.get(2)),
            extractTexto(tds.get(3)),
            extractTexto(tds.get(4)),
            extractUrl(tds.get(0))
        );
        proyectos.add(pl);
      }
    } catch (Exception e) {
      LOG.error("Error on {} with table {}", url, table, e);
    }
    return proyectos;
  }

  private String extractTexto(Element element) {
    return element.select("font").text();
  }

  private LocalDate extractFecha(Element element) {
    var text = element.select("font").text();
    if (text.isBlank()) return null;
    return LocalDate.parse(text, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
  }

  private int extractNumero(Element element) {
    String txt = element.select("a").text();
    return Integer.parseInt(txt);
  }

  private String extractUrl(Element element) {
    return BASE_URL_V1 + element.select("a").attr("href");
  }

  public static void main(String[] args) throws IOException {
    //var pls = new ProyectosLeyExtraction().apply(Periodo._1995_2000);
    var pls = new ProyectosLeyExtractionV1().apply(Periodo._2000_2001);
    //var pls = new ProyectosLeyExtraction().apply(Periodo._2001_2006);
    //var pls = new ProyectosLeyExtraction().apply(Periodo._2006_2011);
    //var pls = new ProyectosLeyExtraction().apply(Periodo._2011_2016);
    //var pls = new ProyectosLeyExtraction().apply(Periodo._2016_2021);
    System.out.println(pls);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(pls.proyectos());
    System.out.println(json);
    Files.writeString(Path.of("target/pl.json"), json);
  }
}