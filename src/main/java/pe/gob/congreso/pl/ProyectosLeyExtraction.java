package pe.gob.congreso.pl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProyectosLeyExtraction {

  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyExtraction.class);

  public ProyectosLey load(final Periodo periodo) throws IOException {
    var pls = new ProyectosLey(periodo);

    var index = 1;
    var url = periodo.baseUrl + index;

    var proyectos = extractPaginaProyectos(url);
    pls.addAll(proyectos);
    while (proyectos.size() == periodo.batchSize) {
      index = index + periodo.batchSize;
      proyectos = extractPaginaProyectos(periodo.baseUrl + index);
      pls.addAll(proyectos);
    }
    return pls;
  }

  private Set<ProyectosLey.ProyectoLey> extractPaginaProyectos(String url) throws IOException {
    var proyectos = new LinkedHashSet<ProyectosLey.ProyectoLey>();
    var doc = Jsoup.connect(url).get();
    var table = doc.body().select("table[cellpadding=2]").first();
    if (table == null) throw new IllegalStateException("table not found");
    try {
      var trs = table.select("tr[valign=top]");
      for (var tr : trs) {
        var tds = tr.select("td");
        var pl = new ProyectosLey.ProyectoLey(
            extractNumero(tds.get(0)),
            extractFecha(tds.get(1)),
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
    return element.select("a").attr("href");
  }

  public static void main(String[] args) throws IOException {
    var pls = new ProyectosLeyExtraction().load(Periodo._1995_2000);
    System.out.println(pls);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(pls.proyectos);
    System.out.println(json);
  }
}
