package pe.gob.congreso.pl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ProyectosLeyMetadata {

  public final Periodo periodo;
  Set<ProyectoLeyMetadata> proyectos = new LinkedHashSet<>();

  public ProyectosLeyMetadata(Periodo periodo) {
    this.periodo = periodo;
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
      String titulo,
      String estadoActual,
      LocalDate fechaPresentacion,
      Optional<String> legislatura,
      Optional<String> proponente,
      Optional<String> sumilla,
      Optional<String> grupoParlamentario,

      Optional<Congresista> autores,
      Set<Congresista> coAutores,
      Set<Congresista> adherentes,

      Set<Seguimiento> seguimientos,
      Set<String> comisiones,
      Optional<String> comisionActual,

      Optional<String> urlExpediente
  ) {
    public static ProyectoLeyMetadata from(ProyectosLey.ProyectoLey pl) {
      return new ProyectoLeyMetadata(
          pl.periodo(),
          pl.numero(),
          Optional.empty(),
          pl.titulo(),
          pl.estado(),
          pl.presentacion(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Set.of(),
          Set.of(),
          Set.of(),
          Set.of(),
          Optional.empty(),
          Optional.ofNullable(pl.url())
      );
    }

    public String id() {
      var f = "%06d".formatted(numero);
      return "%d-%d-%s".formatted(periodo.desde(), periodo.hasta(), f);
    }
  }

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
