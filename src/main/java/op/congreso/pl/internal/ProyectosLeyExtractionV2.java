package op.congreso.pl.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import op.congreso.pl.Constants;
import op.congreso.pl.Periodo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import op.congreso.pl.ProyectosLey;

public class ProyectosLeyExtractionV2  implements Function<Periodo, ProyectosLey> {
  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyExtractionV2.class);
  HttpClient httpClient = HttpClient.newBuilder().build();
  ObjectMapper mapper = new ObjectMapper();

  @Override public ProyectosLey apply(Periodo periodo) {
    LOG.info("Iniciando extraccion de lista de proyectos de ley");
    try {
      var pls = new ProyectosLey(periodo);

      var requestJson = mapper.createObjectNode()
          .put("perParId", periodo.desde());
      final HttpRequest request = HttpRequest.newBuilder()
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestJson)))
          .header("Content-Type", "application/json")
          .header("User-Agent", "Mozilla/5.0 Firefox/26.0")
          .uri(URI.create(periodo.baseUrl()))
          .build();
      LOG.info("Request: " + request);
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) throw new IllegalStateException("Error on POST request, response code: " + response.statusCode());
      var responseJson = mapper.readTree(response.body());
      if (responseJson.get("code").asInt() != 200 ||
          !responseJson.get("status").textValue().equals("success")) {
        throw new IllegalStateException("Error on POST request, response: " + mapper.writeValueAsString(requestJson));
      }
      var data = responseJson.get("data");
      if (data instanceof ArrayNode) { // v1
        for (var item : data) {
          var num = item.get("pleyNum").asInt();
          pls.add(new ProyectosLey.ProyectoLey(
                  periodo,
                  num,
                  Optional.empty(),
                  LocalDate.parse(item.get("fecPresentacion").textValue(),
                          DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                  item.get("desEstado").textValue(),
                  item.get("titulo").textValue(),
                  Constants.BASE_URL_V2 + "/spley-portal/#/expediente/%s/%s".formatted(periodo.desde(), num)
          ));
        }
      }
      if (data instanceof ObjectNode) { // v2
        for (var item : data.get("proyectos")) {
          var num = item.get("pleyNum").asInt();
          var fecPresentacion = item.get("fecPresentacion").textValue();
          pls.add(new ProyectosLey.ProyectoLey(
                  periodo,
                  num,
                  Optional.empty(),
                  LocalDate.parse(fecPresentacion.substring(0, fecPresentacion.indexOf("T")),
                          DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                  item.get("desEstado").textValue(),
                  item.get("titulo").textValue(),
                  Constants.BASE_URL_V2 + "/spley-portal/#/expediente/%s/%s".formatted(periodo.desde(), num)
          ));
        }
      }


      LOG.info("{} PLs extracted", pls.proyectos().size());
      return pls;
    } catch (Exception e) {
      throw new RuntimeException("Error", e);
    }
  }

  public static void main(String[] args) throws IOException {
    var pls = new ProyectosLeyExtractionV2().apply(Periodo._2021_2026);
    System.out.println(pls);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(pls.proyectos());
    System.out.println(json);
    Files.writeString(Path.of("target/pl.json"), json);
  }
}
