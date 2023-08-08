package op.congreso.pl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import op.congreso.pl.internal.ProyectosLeyExtractionV1;
import op.congreso.pl.internal.ProyectosLeyLoadSqlite;
import op.congreso.pl.internal.ProyectosLeyMetadataExtractionV1;
import op.congreso.pl.internal.ProyectosLeyMetadataExtractionV2;
import op.congreso.pl.internal.ProyectosLeyExtractionV2;

import static op.congreso.pl.Constants.BASE_URL_V1;
import static op.congreso.pl.Constants.BASE_URL_V2;

public enum Periodo {
  _1995_2000(1995, 2000,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey1995.nsf/Local Por Numero?OpenView&Start=",
      30,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2000_2001(2000, 2001,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2000.nsf/Por Numero?OpenView&Start=",
      30,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2001_2006(2001, 2006,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2001.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2006_2011(2006, 2011,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2006.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2011_2016(2011, 2016,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2011.nsf/Local Por Numero?OpenView&Start=",
      1000,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2016_2021(2016, 2021,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2016.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1(),
      new ProyectosLeyMetadataExtractionV1()),
  _2021_2026(2021, 2026,
      BASE_URL_V2 + "/spley-portal-service/proyecto-ley/lista-con-filtro",
      -1,
      new ProyectosLeyExtractionV2(),
      new ProyectosLeyMetadataExtractionV2()),
  ;

  final int desde;
  final int hasta;
  final String baseUrl;
  final int batchSize;
  final Function<Periodo, ProyectosLey> extractProyectosLeyFunction;
  final Function<ProyectosLey, ProyectosLeyMetadata> extractMetadataFunction;

  Periodo(int desde, int hasta, String baseUrl, int batchSize,
      Function<Periodo, ProyectosLey> extractProyectosLeyFunction,
      Function<ProyectosLey, ProyectosLeyMetadata> extractMetadataFunction) {
    this.desde = desde;
    this.hasta = hasta;
    this.baseUrl = baseUrl;
    this.batchSize = batchSize;
    this.extractProyectosLeyFunction = extractProyectosLeyFunction;
    this.extractMetadataFunction = extractMetadataFunction;
  }

  public int desde() {
    return desde;
  }

  public int hasta() {
    return hasta;
  }

  public String baseUrl() {
    return baseUrl;
  }

  public int batchSize() {
    return batchSize;
  }

  public ProyectosLeyMetadata collect() {
    return extractMetadataFunction.apply(extractProyectosLeyFunction.apply(this));
  }

  public String texto() {
    return desde + "-" + hasta;
  }

  public Path path() {
    return Path.of(filename() + ".json");
  }

  public String filename() {
    return "proyectos-ley-" + this.texto();
  }

  public void importJson() throws IOException {
    var c = collect();
    Files.writeString(path(), c.json());
  }

  public void exportDbFromJson() throws IOException {
    new ProyectosLeyLoadSqlite().accept(loadFromJson());
  }

  public void load() {
    new ProyectosLeyLoadSqlite().accept(collect());
  }

  private ProyectosLeyMetadata loadFromJson() throws IOException {
    var json = Files.readString(path());
    return new ProyectosLeyMetadata(this).loadJson(json);
  }

  public String periodoId(String id) {
    var i = id.trim();
    if (i.length() == 4) i = "0".concat(i);
    return "%d-%d-%s".formatted(desde(), hasta(), i);
  }
}
