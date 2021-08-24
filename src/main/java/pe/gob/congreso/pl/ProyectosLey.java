package pe.gob.congreso.pl;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class ProyectosLey {

  final Periodo periodo;
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

  record ProyectoLey (
      int numero,
      LocalDate ultimaModificacion,
      LocalDate presentacion,
      String estado,
      String titulo,
      String url
  ) {}
}
