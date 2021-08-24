package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

import static pe.gob.congreso.pl.Constants.BASE_URL_V2;

public class ProyectosLeyMetadataExtractionV2 {

  static HttpClient httpClient = HttpClient.newBuilder().build();
  static ObjectMapper mapper = new ObjectMapper();

  static class ProyectoLeyMetadataExtraction
      implements Function<ProyectosLey.ProyectoLey, ProyectosLeyMetadata.ProyectoLeyMetadata> {

    @Override
    public ProyectosLeyMetadata.ProyectoLeyMetadata apply(ProyectosLey.ProyectoLey pl) {
      try {
        var response = httpClient.send(
            HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL_V2 + "/spley-portal-service/expediente/%s/%s"
                    .formatted(pl.periodo().desde(), pl.numero())))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Response fail");
        var responseJson = mapper.readTree(response.body());
        if (responseJson.get("code").intValue() != 200 ||
            !responseJson.get("status").textValue().equals("success")) {
          throw new IllegalStateException("Response value fail");
        }

        var data = responseJson.get("data");

        var firmantes = firmantes((ArrayNode) data.get("firmantes"));

        var seguimientos = seguimientos((ArrayNode) data.get("seguimientos"));

        return new ProyectosLeyMetadata.ProyectoLeyMetadata(
            pl.periodo(),
            data.get("general").get("pleyId").asInt(),
            Optional.of(data.get("general").get("proyectoLey").textValue()),
            data.get("general").get("desLegis").textValue(),
            LocalDate.parse(data.get("general").get("fecPresentacion").asText(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            data.get("general").get("desProponente").textValue(),
            data.get("general").get("titulo").textValue(),
            Optional.ofNullable(data.get("general").get("sumilla")).map(JsonNode::textValue),
            Optional.ofNullable(data.get("general").get("desGpar")).map(JsonNode::textValue),
            data.get("general").get("desEstado").textValue(),

            firmantes.get(1).stream().findAny(),
            firmantes.get(2),
            firmantes.get(3),

            seguimientos,
            seguimientos.stream().map(ProyectosLeyMetadata.Seguimiento::comision)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet())
        );
      } catch (Exception e) {
        throw new RuntimeException("Error", e);
      }
    }

    private Set<ProyectosLeyMetadata.Seguimiento> seguimientos(ArrayNode seguimientos) {
      var s = new LinkedHashSet<ProyectosLeyMetadata.Seguimiento>();
      for (var item : seguimientos) {
        s.add(new ProyectosLeyMetadata.Seguimiento(
            LocalDate.parse(item.get("fecha").asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZ")),
            item.get("desEstado").textValue(),
            Optional.ofNullable(item.get("desComision")).map(JsonNode::textValue),
            item.get("detalle").textValue()
        ));
      }
      return s;
    }

    private Map<Integer, Set<ProyectosLeyMetadata.Congresista>> firmantes(ArrayNode firmantes) {
      Map<Integer, Set<ProyectosLeyMetadata.Congresista>> map =
          new HashMap<>(Map.of(
              1, new LinkedHashSet<>(),
              2, new LinkedHashSet<>(),
              3, new LinkedHashSet<>()));
      for (var item : firmantes) {
        var i = item.get("tipoFirmanteId").intValue();
        map.computeIfPresent(i, (tipo, congresistas) -> {
          congresistas.add(
              new ProyectosLeyMetadata.Congresista(
                  item.get("nombre").textValue(),
                  Optional.of(item.get("dni")).map(JsonNode::textValue),
                  Optional.of(item.get("sexo")).map(JsonNode::textValue),
                  Optional.of(item.get("pagWeb")).map(JsonNode::textValue)
              )
          );
          return congresistas;
        });
      }
      return map;
    }
  }

  public static void main(String[] args) {
    var meta = new ProyectoLeyMetadataExtraction().apply(new ProyectosLey.ProyectoLey(Periodo._2021_2026,
        6,
        Optional.empty(),
        LocalDate.now(),
        "", "", ""));
    System.out.println(meta);
  }
}
