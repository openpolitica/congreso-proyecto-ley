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
      Optional<String> numeroPeriodo,
      String legislatura,
      LocalDate fechaPresentacion,
      String proponente,
      String titulo,
      Optional<String> sumilla,
      Optional<String> grupoParlamentario,
      String estadoActual,

      Optional<Congresista> autores,
      Set<Congresista> coAutores,
      Set<Congresista> adherentes,

      Set<Seguimiento> seguimientos,
      Set<String> comisiones
  ) {}

  public record Congresista (
      String nombreCompleto,
      Optional<String> dni,
      Optional<String> sexo,
      Optional<String> url
  ) {}

  public record Seguimiento (
    LocalDate fecha,
    String detalle,
    Optional<String> estado,
    Optional<String> comision
  ) {}
}
