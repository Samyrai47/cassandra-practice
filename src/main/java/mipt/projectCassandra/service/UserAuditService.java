package mipt.projectCassandra.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mipt.projectCassandra.config.CassandraConfiguration;
import mipt.projectCassandra.connector.CassandraConnector;
import mipt.projectCassandra.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuditService {
  private final CassandraConfiguration cassandraConfiguration;

  public void initializeKeyspace() {
    CassandraConnector client = new CassandraConnector();
    client.connect(
        cassandraConfiguration.getContactPoints(),
        cassandraConfiguration.getPort(),
        cassandraConfiguration.getLocalDatacenter());

    CqlSession session = client.getSession();

    SimpleStatement statement =
        SimpleStatement.newInstance(
            "CREATE KEYSPACE IF NOT EXISTS "
                + cassandraConfiguration.getKeyspaceName()
                + " WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};");

    session.execute(statement);

    client.close();
  }

  public void initializeTable() {
    CassandraConnector client = new CassandraConnector();
    client.connect(
        cassandraConfiguration.getContactPoints(),
        cassandraConfiguration.getPort(),
        cassandraConfiguration.getLocalDatacenter());

    CqlSession session = client.getSession();

    SimpleStatement statement =
        SimpleStatement.newInstance(
            "CREATE TABLE IF NOT EXISTS "
                + cassandraConfiguration.getKeyspaceName()
                + ".user_audit (user_id UUID, event_time TIMESTAMP, event_type TEXT, event_details TEXT, PRIMARY KEY ((user_id), event_time)) "
                + "WITH CLUSTERING ORDER BY (event_time DESC) AND default_time_to_live = 31536000;");

    session.execute(statement);

    client.close();
  }

  public void createEventAudit(UUID userId, Action action) {
    if (userId == null || action == null) {
      throw new IllegalArgumentException(
          userId == null ? "UUID shouldn't be null" : "Action shouldn't be null");
    }

    CassandraConnector client = new CassandraConnector();
    client.connect(
        cassandraConfiguration.getContactPoints(),
        cassandraConfiguration.getPort(),
        cassandraConfiguration.getLocalDatacenter());

    CqlSession session = client.getSession();

    PreparedStatement preparedStatement =
        session.prepare(
            "INSERT INTO "
                + cassandraConfiguration.getKeyspaceName()
                + ".user_audit (user_id, event_time, event_type, event_details) "
                + "VALUES (?, ?, ?, ?)");

    BoundStatement boundStatement =
        preparedStatement.bind(
            userId, java.time.Instant.now(), action.toString(), "User UPDATE from IP 192.168.1.1");

    session.execute(boundStatement);

    client.close();
  }

  public String readUserAudit(UUID userId) {
    CassandraConnector client = new CassandraConnector();
    client.connect(
        cassandraConfiguration.getContactPoints(),
        cassandraConfiguration.getPort(),
        cassandraConfiguration.getLocalDatacenter());

    CqlSession session = client.getSession();

    PreparedStatement statement =
        session.prepare(
            "SELECT * FROM "
                + cassandraConfiguration.getKeyspaceName()
                + ".user_audit WHERE user_id = ?;");
    BoundStatement boundStatement = statement.bind(userId);

    ResultSet rows = session.execute(boundStatement);

    if (!rows.iterator().hasNext()) {
      throw new UserNotFoundException("User with uuid " + userId + " wasn't found");
    }

    client.close();

    return rows.one().getString("event_details");
  }
}
