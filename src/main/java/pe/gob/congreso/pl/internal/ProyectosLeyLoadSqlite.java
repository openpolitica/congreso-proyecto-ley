package pe.gob.congreso.pl.internal;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

public class ProyectosLeyLoadSqlite implements Consumer<ProyectosLeyMetadata> {

  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyLoadSqlite.class);

  static List<TableLoad> tableLoadList = List.of(
      new ProyectoTableLoad(),
      new SeguimientoTableLoad(),
      new FirmanteTableLoad()
  );

  @Override public void accept(ProyectosLeyMetadata meta) {
    var jdbcUrl = "jdbc:sqlite:proyectos-ley-%s.db".formatted(meta.periodo.texto());
    try (var connection = DriverManager.getConnection(jdbcUrl)) {
      var statement = connection.createStatement();
      for (var tableLoad : tableLoadList) {
        LOG.info("Loading {}", tableLoad.tableName);
        statement.executeUpdate(tableLoad.dropTableStatement());
        statement.executeUpdate(tableLoad.createTableStatement());
        LOG.info("Table {} created", tableLoad.tableName);

        var ps = connection.prepareStatement(tableLoad.prepareStatement());
        LOG.info("Statement for {} prepared", tableLoad.tableName);

        for (var m : meta.proyectos()) tableLoad.addBatch(ps, m);

        LOG.info("Batch for {} ready", tableLoad.tableName);
        ps.executeBatch();
        LOG.info("Table {} updated", tableLoad.tableName);
      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  abstract static class TableLoad {
    final String tableName;

    public TableLoad(String tableName) {
      this.tableName = tableName;
    }

    String dropTableStatement() {
      return "drop table if exists %s".formatted(tableName);
    }

    abstract String createTableStatement();

    abstract String prepareStatement();

    abstract void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException;
  }

  static class ProyectoTableLoad extends TableLoad {
    public ProyectoTableLoad() {
      super("proyecto_ley");
    }

    @Override String createTableStatement() {
      return """
          create table %s (
            id text primary key,
            periodo text not null,
            numero integer not null,
            numero_periodo text,
            legislatura text,
            presentacion_fecha text not null,
            proponente text,
            grupo_parlamentario text,
            estado_actual text not null,
            
            titulo text not null,
            sumilla text,
            
            comision_actual text,
            expediente_url text
          )
          """.formatted(tableName);
    }

    @Override String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException {
      ps.setString(1, pl.id());
      ps.setString(2, pl.periodo().texto());
      ps.setInt(3, pl.numero());
      if (pl.numeroPeriodo().isPresent()) ps.setString(4, pl.numeroPeriodo().get());
      if (pl.legislatura().isPresent()) ps.setString(5, pl.legislatura().get());
      ps.setString(6, pl.fechaPresentacion().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
      if (pl.proponente().isPresent()) ps.setString(7, pl.proponente().get());
      if (pl.grupoParlamentario().isPresent()) ps.setString(8, pl.grupoParlamentario().get());
      ps.setString(9, pl.estadoActual());
      ps.setString(10, pl.titulo());
      if (pl.sumilla().isPresent()) ps.setString(11, pl.sumilla().get());
      if (pl.comisionActual().isPresent()) ps.setString(12, pl.comisionActual().get());
      if (pl.urlExpediente().isPresent()) ps.setString(13, pl.urlExpediente().get());
      ps.addBatch();
    }
  }

  static class SeguimientoTableLoad extends TableLoad {
    public SeguimientoTableLoad() {
      super("seguimiento");
    }

    @Override String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override String createTableStatement() {
      return """
          create table %s (
            proyecto_ley_id text,
            fecha text not null,
            detalle text not null,
            comision text,
            estado text,
            FOREIGN KEY(proyecto_ley_id) REFERENCES proyecto_ley(id)
          )
          """.formatted(tableName);
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException {
      for (var s : pl.seguimientos()) {
        ps.setString(1, pl.id());
        ps.setString(2, s.fecha().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        ps.setString(3, s.detalle());
        if (s.comision().isPresent()) ps.setString(4, s.comision().get());
        if (s.estado().isPresent()) ps.setString(5, s.estado().get());
        ps.addBatch();
      }
    }
  }

  static class FirmanteTableLoad extends TableLoad {
    public FirmanteTableLoad() {
      super("firmante");
    }

    @Override String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override String createTableStatement() {
      return """
          create table %s (
            proyecto_ley_id text,
            congresista text not null,
            firmante_tipo text not null,
            FOREIGN KEY(proyecto_ley_id) REFERENCES proyecto_ley(id)
          )
          """.formatted(tableName);
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException {
      if (pl.autores().isPresent()) {
        ps.setString(1, pl.id());
        ps.setString(2, pl.autores().get().nombreCompleto());
        ps.setString(3, "AUTOR");
        ps.addBatch();
      }
      for (var f : pl.coAutores()) {
        ps.setString(1, pl.id());
        ps.setString(2, f.nombreCompleto());
        ps.setString(3, "COAUTOR");
        ps.addBatch();
      }
      for (var f : pl.adherentes()) {
        ps.setString(1, pl.id());
        ps.setString(2, f.nombreCompleto());
        ps.setString(3, "ADHERENTE");
        ps.addBatch();
      }
    }
  }
}
