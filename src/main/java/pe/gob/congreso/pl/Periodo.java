package pe.gob.congreso.pl;

import java.util.function.Function;

import static pe.gob.congreso.pl.Constants.BASE_URL_V1;
import static pe.gob.congreso.pl.Constants.BASE_URL_V2;

enum Periodo {
  _1995_2000(1995, 2000,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey1995.nsf/Local Por Numero?OpenView&Start=",
      30,
      new ProyectosLeyExtractionV1()),
  _2000_2001(2000, 2001,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2000.nsf/Por Numero?OpenView&Start=",
      30,
      new ProyectosLeyExtractionV1()),
  _2001_2006(2001, 2006,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2001.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1()),
  _2006_2011(2006, 2011,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2006.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1()),
  _2011_2016(2011, 2016,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2011.nsf/Local Por Numero?OpenView&Start=",
      1000,
      new ProyectosLeyExtractionV1()),
  _2016_2021(2016, 2021,
      BASE_URL_V1 + "/Sicr/TraDocEstProc/CLProLey2016.nsf/Local Por Numero?OpenView&Start=",
      500,
      new ProyectosLeyExtractionV1()),
  _2021_2026(2021, 2026,
      BASE_URL_V2 + "/spley-portal-service/proyecto-ley/lista-con-filtro",
      -1,
      new ProyectosLeyExtractionV2()),
  ;

  final int desde;
  final int hasta;
  final String baseUrl;
  final int batchSize;
  final Function<Periodo, ProyectosLey> extractProyectosLeyFunction;

  Periodo(int desde, int hasta, String baseUrl, int batchSize,
      Function<Periodo, ProyectosLey> extractProyectosLeyFunction) {
    this.desde = desde;
    this.hasta = hasta;
    this.baseUrl = baseUrl;
    this.batchSize = batchSize;
    this.extractProyectosLeyFunction = extractProyectosLeyFunction;
  }
}
