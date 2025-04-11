package mipt.projectCassandra.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mipt.projectCassandra.config.CassandraConfiguration;
import mipt.projectCassandra.dto.Action;
import mipt.projectCassandra.entity.UserAudit;
import mipt.projectCassandra.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuditService {
  private final CqlSession session;
  private final CassandraConfiguration cassandraConfiguration;

  public void createEventAudit(Long userId, Action action) {
    if (userId == null || action == null) {
      throw new IllegalArgumentException(
          userId == null ? "UUID shouldn't be null" : "Action shouldn't be null");
    }

    PreparedStatement preparedStatement =
        session.prepare(
            "INSERT INTO %s.user_audit (user_id, event_time, event_type, event_details) "
                    .formatted(cassandraConfiguration.getKeyspaceName())
                + "VALUES (?, ?, ?, ?)");

    BoundStatement boundStatement =
        preparedStatement.bind(
            userId,
            java.time.Instant.now(),
            action.toString(),
            "User " + action + " from IP 192.168.1.1");

    log.info("Retrieved insert request: {}, {}", userId, action);
    session.execute(boundStatement);
  }

  public List<UserAudit> readUserAudit(Long userId) {
    PreparedStatement statement =
        session.prepare(
            "SELECT * FROM "
                + cassandraConfiguration.getKeyspaceName()
                + ".user_audit WHERE user_id = ?;");
    BoundStatement boundStatement = statement.bind(userId);

    ResultSet rows = session.execute(boundStatement);

    if (!rows.iterator().hasNext()) {
      throw new UserNotFoundException("User with id " + userId + " wasn't found");
    }

    List<UserAudit> userAudits = new ArrayList<>();
    for (Row row : rows) {
      userAudits.add(
          new UserAudit(
              row.getLong("user_id"),
              row.getInstant("event_time"),
              row.getString("event_details")));
    }
    return userAudits;
  }
}
