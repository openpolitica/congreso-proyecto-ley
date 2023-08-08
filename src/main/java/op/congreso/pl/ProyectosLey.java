package op.congreso.pl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ProyectosLey {

  public final Periodo periodo;
  Set<ProyectoLey> proyectos = new LinkedHashSet<>();

  public ProyectosLey(Periodo periodo) {
    this.periodo = periodo;
  }

  public void add(ProyectoLey proyectoLey) {
    proyectos.add(proyectoLey);
  }

  public void addAll(Set<ProyectoLey> proyectos) {
    this.proyectos.addAll(proyectos);
  }

  public Set<ProyectoLey> proyectos() {
    return Collections.unmodifiableSet(proyectos);
  }

  public record ProyectoLey (
      Periodo periodo,
      int numero,
      Optional<LocalDate> ultimaModificacion,
      LocalDate presentacion,
      String estado,
      String titulo,
      String url
  ) {}
}
