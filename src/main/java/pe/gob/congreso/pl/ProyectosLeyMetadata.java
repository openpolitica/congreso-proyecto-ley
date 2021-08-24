package pe.gob.congreso.pl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ProyectosLeyMetadata {

  final Periodo periodo;
  Set<ProyectoLeyMetadata> proyectos = new LinkedHashSet<>();

  public ProyectosLeyMetadata(Periodo periodo) {
    this.periodo = periodo;
  }

  public void add(ProyectoLeyMetadata proyectoLey) {
    proyectos.add(proyectoLey);
  }

  public void addAll(Set<ProyectoLeyMetadata> proyectos) {
    this.proyectos.addAll(proyectos);
  }

  public Set<ProyectoLeyMetadata> proyectos() {
    return Collections.unmodifiableSet(proyectos);
  }

  public record ProyectoLeyMetadata (
      Periodo periodo,
      int numero,
      String numeroPeriodo,
      String legislatura,
      LocalDate fechaPresentacion,
      String proponente,
      String titulo,
      String sumilla,
      String grupoParlamentario,
      String estadoActual,

      Set<Congresista> autores,
      Set<Congresista> coAutores,
      Set<Congresista> adherentes,

      Set<Seguimiento> seguimientos,
      Set<String> comisiones
  ) {}

  public record Congresista (
      String nombreCompleto,
      String dni,
      String sexo,
      String url
  ) {}

  public record Seguimiento (
    LocalDate fecha,
    String estado,
    Optional<String> comision,
    String detalle
  ) {}
}
