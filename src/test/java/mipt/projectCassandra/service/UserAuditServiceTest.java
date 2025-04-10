package mipt.projectCassandra.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.List;
import java.util.UUID;
import mipt.projectCassandra.entity.UserAudit;
import mipt.projectCassandra.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(
    properties = {
      "spring.data.cassandra.contact-points=${spring.cassandra.contact-points}",
      "spring.data.cassandra.port=${spring.cassandra.port}",
      "spring.data.cassandra.keyspace-name=${spring.cassandra.keyspace-name}",
      "spring.data.cassandra.local-datacenter=datacenter1"
    })
@TestMethodOrder(OrderAnnotation.class)
class UserAuditServiceTest {
  @Autowired private UserAuditService userAuditService;
  @Autowired private CqlSession session;

  @Container
  private static final CassandraContainer cassandraContainer =
      new CassandraContainer("cassandra:3.11.2").withExposedPorts(9042);

  private static final UUID uuid = UUID.randomUUID();

  @BeforeAll
  static void setupCassandraConnectionProperties() {
    System.setProperty("spring.cassandra.keyspace-name", "datacenter1");
    System.setProperty(
        "spring.cassandra.contact-points", cassandraContainer.getContainerIpAddress());
    System.setProperty(
        "spring.cassandra.port", String.valueOf(cassandraContainer.getMappedPort(9042)));

    System.out.println("Cassandra container IP: " + cassandraContainer.getContainerIpAddress());
    System.out.println("Cassandra container port: " + cassandraContainer.getMappedPort(9042));
  }

  @Test
  @Order(1)
  void shouldFailToGetAudit() {
    assertThrows(
        UserNotFoundException.class, () -> userAuditService.readUserAudit(UUID.randomUUID()));
    SimpleStatement statement = SimpleStatement.newInstance("SELECT * FROM datacenter1.user_audit");
    ResultSet resultSet = session.execute(statement);
    assertEquals(false, resultSet.iterator().hasNext());
  }

  @Test
  void shouldSuccessfullyCreateAudit() {
    userAuditService.createEventAudit(uuid, Action.UPDATE);
  }

  @Test
  void shouldFailToCreateAuditWithNullUuid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userAuditService.createEventAudit(null, Action.DROPPED_DATABASE));
  }

  @Test
  void shouldFailToCreateAuditWithNullAction() {
    assertThrows(
        IllegalArgumentException.class, () -> userAuditService.createEventAudit(uuid, null));
  }

  @Test
  void shouldSuccessfullyGetAudit() {
    List<UserAudit> result = userAuditService.readUserAudit(uuid);
    assertEquals("User UPDATE from IP 192.168.1.1", result.getFirst().description());
    assertEquals(uuid, result.getFirst().userId());
  }
}
