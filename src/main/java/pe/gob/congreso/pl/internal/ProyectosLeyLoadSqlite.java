package pe.gob.congreso.pl.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.gob.congreso.pl.ProyectosLeyMetadata;

public class ProyectosLeyLoadSqlite implements Consumer<ProyectosLeyMetadata> {

  static final Logger LOG = LoggerFactory.getLogger(ProyectosLeyLoadSqlite.class);

  static final ObjectMapper mapper = new ObjectMapper();
  public static final String YYYY_MM_DD = "yyyy-MM-dd";

  static List<TableLoad> tableLoadList = List.of(
      new ProyectoTableLoad(),
      new SeguimientoTableLoad(),
      new FirmanteTableLoad(),
      new IniciativaAgrupadaTableLoad()
  );

  @Override public void accept(ProyectosLeyMetadata meta) {
    var jdbcUrl = "jdbc:sqlite:%s.db".formatted(meta.periodo.filename());
    try (var connection = DriverManager.getConnection(jdbcUrl)) {
      var statement = connection.createStatement();
      statement.executeUpdate("pragma journal_mode = WAL");
      statement.executeUpdate("pragma synchronous = off");
      statement.executeUpdate("pragma temp_store = memory");
      statement.executeUpdate("pragma mmap_size = 300000000");
      statement.executeUpdate("pragma page_size = 32768");
      for (var tableLoad : tableLoadList) {
        LOG.info("Loading {}", tableLoad.tableName);
        statement.executeUpdate(tableLoad.dropTableStatement());
        statement.executeUpdate(tableLoad.createTableStatement());
        for (String s : tableLoad.createIndexesStatement()) {
          statement.executeUpdate(s);
        }
        LOG.info("Table {} created", tableLoad.tableName);

        var ps = connection.prepareStatement(tableLoad.prepareStatement());
        LOG.info("Statement for {} prepared", tableLoad.tableName);

        for (var m : meta.proyectos()) tableLoad.addBatch(ps, m);

        LOG.info("Batch for {} ready", tableLoad.tableName);
        ps.executeBatch();
        LOG.info("Table {} updated", tableLoad.tableName);
      }
      statement.executeUpdate("pragma vacuum;");
      statement.executeUpdate("pragma optimize;");
    } catch (Exception throwables) {
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

    abstract List<String> createIndexesStatement();

    String index(String field) {
      return "CREATE INDEX %s_%s ON %s(\"%s\");\n"
          .formatted(tableName, field, tableName, field);
    }

    abstract String prepareStatement();

    abstract void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException, IOException;
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
            ultimo_estado text not null,
            
            titulo text not null,
            sumilla text,
            
            ultima_comision text,
            expediente_url text,
            
            firmantes text,
            autor text,
            coautores text,
            adherentes text,
            
            comisiones text,
            iniciativas_agrupadas text
          )
          """.formatted(tableName);
    }

    @Override List<String> createIndexesStatement() {
      return List.of(
          index("legislatura"),
          index("proponente"),
          index("grupo_parlamentario"),
          index("ultimo_estado"),
          index("ultima_comision"),
          index("autor")
      );
    }

    @Override String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException, IOException {
      ps.setString(1, pl.id());
      ps.setString(2, pl.periodo().texto());
      ps.setInt(3, pl.numero());
      if (pl.numeroPeriodo().isPresent()) ps.setString(4, pl.numeroPeriodo().get());
      else ps.setNull(4, JDBCType.VARCHAR.ordinal());
      if (pl.legislatura().isPresent()) ps.setString(5, pl.legislatura().get());
      else ps.setNull(5, JDBCType.VARCHAR.ordinal());
      ps.setString(6, pl.fechaPresentacion().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
      if (pl.proponente().isPresent()) ps.setString(7, pl.proponente().get());
      else ps.setNull(7, JDBCType.VARCHAR.ordinal());
      if (pl.grupoParlamentario().isPresent()) ps.setString(8, pl.grupoParlamentario().get());
      else ps.setNull(8, JDBCType.VARCHAR.ordinal());
      ps.setString(9, pl.estadoActual());
      ps.setString(10, pl.titulo());
      if (pl.sumilla().isPresent()) ps.setString(11, pl.sumilla().get());
      else ps.setNull(11, JDBCType.VARCHAR.ordinal());
      if (pl.comisionActual().isPresent()) ps.setString(12, pl.comisionActual().get());
      else ps.setNull(12, JDBCType.VARCHAR.ordinal());
      if (pl.urlExpediente().isPresent()) ps.setString(13, pl.urlExpediente().get());
      else ps.setNull(13, JDBCType.VARCHAR.ordinal());
      if (!pl.firmantes().isEmpty()) ps.setString(14,
          mapper.writeValueAsString(pl.firmantes().stream()
              .map(ProyectosLeyMetadata.Congresista::nombreCompleto)
              .collect(Collectors.toSet())));
      else ps.setNull(14, JDBCType.VARCHAR.ordinal());
      if (pl.autor().isPresent()) ps.setString(15, pl.autor().get().nombreCompleto());
      else ps.setNull(15, JDBCType.VARCHAR.ordinal());
      if (!pl.coAutores().isEmpty())
        ps.setString(16,
          mapper.writeValueAsString(pl.coAutores().stream()
              .map(ProyectosLeyMetadata.Congresista::nombreCompleto)
              .collect(Collectors.toSet())));
      else ps.setNull(16, JDBCType.VARCHAR.ordinal());
      if (!pl.adherentes().isEmpty()) ps.setString(17,
          mapper.writeValueAsString(pl.adherentes().stream()
              .map(ProyectosLeyMetadata.Congresista::nombreCompleto)
              .collect(Collectors.toSet())));
      else ps.setNull(17, JDBCType.VARCHAR.ordinal());
      if (!pl.comisiones().isEmpty())
        ps.setString(18, mapper.writeValueAsString(pl.comisiones()));
      else ps.setNull(18, JDBCType.VARCHAR.ordinal());
      if (!pl.iniciativasAgrupadas().isEmpty())
        ps.setString(19, mapper.writeValueAsString(pl.iniciativasAgrupadas()));
      else ps.setNull(19, JDBCType.VARCHAR.ordinal());

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

    @Override List<String> createIndexesStatement() {
      return List.of(
          index("comision")
      );
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException {
      for (var s : pl.seguimientos()) {
        ps.setString(1, pl.id());
        ps.setString(2, s.fecha().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        ps.setString(3, s.detalle());
        if (s.comision().isPresent()) ps.setString(4, s.comision().get());
        else ps.setNull(4, JDBCType.VARCHAR.ordinal());
        if (s.estado().isPresent()) ps.setString(5, s.estado().get());
        else ps.setNull(5, JDBCType.VARCHAR.ordinal());
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

    @Override List<String> createIndexesStatement() {
      return List.of(
          index("congresista"),
          index("firmante_tipo")
      );
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
        throws SQLException {
      if (pl.autor().isPresent()) {
        ps.setString(1, pl.id());
        ps.setString(2, pl.autor().get().nombreCompleto());
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

  static class IniciativaAgrupadaTableLoad extends TableLoad {
    public IniciativaAgrupadaTableLoad() {
      super("iniciativa_agrupada");
    }

    @Override String prepareStatement() {
      return """
          insert into %s values (
            ?, ?
          )
          """.formatted(tableName);
    }

    @Override String createTableStatement() {
      return """
          create table %s (
            proyecto_ley_id text not null,
            iniciativa_agrupada text not null,
            FOREIGN KEY(proyecto_ley_id) REFERENCES proyecto_ley(id),
            FOREIGN KEY(iniciativa_agrupada) REFERENCES proyecto_ley(id)
          )
          """.formatted(tableName);
    }

    @Override List<String> createIndexesStatement() {
      return List.of(
      );
    }

    @Override void addBatch(PreparedStatement ps, ProyectosLeyMetadata.ProyectoLeyMetadata pl)
            throws SQLException {
      for (var i : pl.iniciativasAgrupadas()) {
        if (!i.isBlank()) {
          ps.setString(1, pl.id());
          ps.setString(2, pl.periodo().periodoId(i));
          ps.addBatch();
        }
      }
    }
  }
}
