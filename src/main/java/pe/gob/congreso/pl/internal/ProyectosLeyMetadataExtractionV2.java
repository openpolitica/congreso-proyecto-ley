package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.congreso.pl.Periodo;
import pe.gob.congreso.pl.ProyectosLey;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

import static pe.gob.congreso.pl.Constants.BASE_URL_V2;

public class ProyectosLeyMetadataExtractionV2 implements Function<ProyectosLey, ProyectosLeyMetadata> {

  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyMetadataExtractionV2.class);

  static HttpClient httpClient = HttpClient.newBuilder().build();
  static ObjectMapper mapper = new ObjectMapper();

  @Override public ProyectosLeyMetadata apply(ProyectosLey proyectosLey) {
    LOG.info("Extracting PL metadata");
    var meta = new ProyectosLeyMetadata(proyectosLey.periodo);
    meta.addAll(proyectosLey.proyectos().stream()
            .parallel()
            .map(p -> Retry.decorateFunction(
                            Retry.of("importar", RetryConfig.custom()
                                    .maxAttempts(3)
                                    .retryExceptions(RuntimeException.class)
                                    .waitDuration(Duration.ofSeconds(10))
                                    .build()),
                            new ProyectoLeyMetadataExtraction())
                    .apply(p)
            )
        .collect(Collectors.toSet()));
    LOG.info("{} PL metadata extracted", meta.proyectos().size());
    return meta;
  }

  static class ProyectoLeyMetadataExtraction
      implements Function<ProyectosLey.ProyectoLey, ProyectosLeyMetadata.ProyectoLeyMetadata> {

    @Override
    public ProyectosLeyMetadata.ProyectoLeyMetadata apply(ProyectosLey.ProyectoLey pl) {
      try {
        var url = BASE_URL_V2 + "/spley-portal-service/expediente/%s/%s"
            .formatted(pl.periodo().desde(), pl.numero());
        var response = httpClient.send(
            HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
          LOG.warn("PL {}/{} not found. URL: {}", pl.periodo().texto(), pl.numero(), url);
          return ProyectosLeyMetadata.ProyectoLeyMetadata.from(pl);
        }
        if (response.statusCode() != 200) throw new IllegalStateException("Response fail: " + response.statusCode() + " :: " + response.body());
        var responseJson = mapper.readTree(response.body());
        if (responseJson.get("code").intValue() != 200 ||
            !responseJson.get("status").textValue().equals("success")) {
          throw new IllegalStateException("Response value fail");
        }

        var data = responseJson.get("data");

        var firmantes = firmantes((ArrayNode) data.get("firmantes"));

        var seguimientos = seguimientos((ArrayNode) data.get("seguimientos"));

        var comisiones = comisiones((ArrayNode) data.get("comisiones"));

//        var comisiones = seguimientos.stream().map(ProyectosLeyMetadata.Seguimiento::comision)
//            .filter(Optional::isPresent)
//            .map(Optional::get)
//            .collect(Collectors.toSet());

        var iniciativasAcumuladas = new LinkedHashSet<String>();
        for (var i : data.get("acumulados")) {
          String proyectoLey = i.get("proyectoLey").textValue();
          iniciativasAcumuladas.add(proyectoLey.split("/")[0]);
        }
        return new ProyectosLeyMetadata.ProyectoLeyMetadata(
            pl.periodo(),
            data.get("general").get("pleyId").asInt(),
            Optional.of(data.get("general").get("proyectoLey").textValue()),
            data.get("general").get("titulo").textValue(),
            data.get("general").get("desEstado").textValue(),
            LocalDate.parse(data.get("general").get("fecPresentacion").asText(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            Optional.ofNullable(data.get("general").get("desLegis")).map(JsonNode::textValue),
            Optional.ofNullable(data.get("general").get("desProponente")).map(JsonNode::textValue),
            Optional.ofNullable(data.get("general").get("sumilla")).map(JsonNode::textValue),
            Optional.ofNullable(data.get("general").get("desGpar")).map(JsonNode::textValue),

            firmantes.get(1).stream().findAny(),
            firmantes.get(2),
            firmantes.get(3),

            seguimientos,
            comisiones,
            comisiones.stream().map(ProyectosLeyMetadata.Comision::nombre).reduce((s, s2) -> s2),

            Optional.of(pl.url()),
            iniciativasAcumuladas
        );
      } catch (Exception e) {
        throw new RuntimeException("Error", e);
      }
    }

    private Set<ProyectosLeyMetadata.Comision> comisiones(ArrayNode comisiones) {
      final var list = new HashSet<ProyectosLeyMetadata.Comision>(comisiones.size());
      for (final var comision : comisiones) {
        list.add(new ProyectosLeyMetadata.Comision(comision.get("id").asInt(), comision.get("nombre").asText()));
      }
      return list;
    }

    private Set<ProyectosLeyMetadata.Seguimiento> seguimientos(ArrayNode seguimientos) {
      var s = new LinkedHashSet<ProyectosLeyMetadata.Seguimiento>();
      for (var item : seguimientos) {
        s.add(new ProyectosLeyMetadata.Seguimiento(
            LocalDate.parse(item.get("fecha").asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZ")),
            item.get("detalle").textValue(),
            Optional.ofNullable(item.get("desEstado")).map(JsonNode::textValue),
            Optional.ofNullable(item.get("desComisiones")).map(JsonNode::textValue)
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
        101,
        Optional.empty(),
        LocalDate.now(),
        "", "", ""));
    System.out.println(meta);
  }
}
