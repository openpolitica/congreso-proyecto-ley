package pe.gob.congreso.pl;

enum Periodo {
  _1995_2000(1995, 2000,
      "https://www2.congreso.gob.pe/Sicr/TraDocEstProc/CLProLey1995.nsf/Local Por Numero?OpenView&Start=",
      30),
  _2000_2001(2000, 2001,
      "",
      100),
  _2001_2006(2001, 2006,
      "",
      100),
  _2006_2011(2006, 2011,
      "",
      100),
  _2011_2016(2011, 2016,
      "",
      100),
  _2016_2021(2016, 2021,
      "",
      100),
  _2021_2026(2021, 2026,
      "",
      100),
  ;

  final int desde;
  final int hasta;
  final String baseUrl;
  final int batchSize;

  Periodo(int desde, int hasta, String baseUrl, int batchSize) {
    this.desde = desde;
    this.hasta = hasta;
    this.baseUrl = baseUrl;
    this.batchSize = batchSize;
  }
}
