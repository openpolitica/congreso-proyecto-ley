package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;

import static pe.gob.congreso.pl.Constants.BASE_URL_V2;

public class ProyectosLeyExtractionV2  implements Function<Periodo, ProyectosLey> {
  HttpClient httpClient = HttpClient.newBuilder().build();
  ObjectMapper mapper = new ObjectMapper();

  @Override public ProyectosLey apply(Periodo periodo) {
    try {
      var pls = new ProyectosLey(periodo);

      var requestJson = mapper.createObjectNode()
          .put("perParId", periodo.desde());
      var response = httpClient.send(
          HttpRequest.newBuilder()
              .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestJson)))
              .header("Content-Type", "application/json")
              .uri(URI.create(periodo.baseUrl()))
              .build(),
          HttpResponse.BodyHandlers.ofString());
      System.out.println(response.body());
      if (response.statusCode() != 200) throw new IllegalStateException("Error on query");
      var responseJson = mapper.readTree(response.body());
      if (responseJson.get("code").asInt() != 200 ||
          !responseJson.get("status").textValue().equals("success")) {
        throw new IllegalStateException("Error on response");
      }
      var dataArray = (ArrayNode) responseJson.get("data");
      for (var item : dataArray) {
        var num = item.get("pleyNum").asInt();
        pls.add(new ProyectosLey.ProyectoLey(
            num,
            Optional.empty(),
            LocalDate.parse(item.get("fecPresentacion").textValue(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            item.get("desEstado").textValue(),
            item.get("titulo").textValue(),
            BASE_URL_V2 + "/spley-portal/#/expediente/%s/%s".formatted(periodo.desde(), num)
        ));
      }

      return pls;
    } catch (Exception e) {
      throw new RuntimeException("Error", e);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var pls = new ProyectosLeyExtractionV2().apply(Periodo._2021_2026);
    System.out.println(pls);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(pls.proyectos());
    System.out.println(json);
    Files.writeString(Path.of("target/pl.json"), json);
  }
}
