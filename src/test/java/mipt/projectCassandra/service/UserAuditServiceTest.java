package mipt.projectCassandra.service;

import static org.junit.Assert.assertThrows;

import mipt.projectCassandra.dto.Action;
import mipt.projectCassandra.exception.UserNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class UserAuditServiceTest {
  @Autowired private UserAuditService userAuditService;

  @Container
  private static final CassandraContainer cassandraContainer =
      new CassandraContainer("cassandra:3.11.2").withExposedPorts(9042);

  private static final Long id = 12L;

  @BeforeAll
  static void setupCassandraConnectionProperties() {
    System.setProperty("spring.cassandra.keyspace-name", "ulixes_keyspace");
    System.setProperty(
        "spring.cassandra.contact-points", cassandraContainer.getContainerIpAddress());
    System.setProperty(
        "spring.cassandra.port", String.valueOf(cassandraContainer.getMappedPort(9042)));

    System.out.println("Cassandra container IP: " + cassandraContainer.getContainerIpAddress());
    System.out.println("Cassandra container port: " + cassandraContainer.getMappedPort(9042));
  }

  @BeforeEach
  void setUp() {
    userAuditService.initializeKeyspace();
    userAuditService.initializeTable();
  }

  @Test
  void shouldSuccessfullyCreateAudit() {
    userAuditService.createEventAudit(id, Action.DROPPED_DATABASE);
  }

  @Test
  void shouldFailToCreateAuditWithNullUuid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userAuditService.createEventAudit(null, Action.DROPPED_DATABASE));
  }

  @Test
  void shouldFailToCreateAuditWithNullAction() {
    assertThrows(IllegalArgumentException.class, () -> userAuditService.createEventAudit(id, null));
  }

  @Test
  void shouldSuccessfullyGetAudit() {
    String result = userAuditService.readUserAudit(id);
    Assertions.assertEquals("User DROPPED_DATABASE from IP 192.168.1.1", result);
  }

  @Test
  void shouldFailToGetAudit() {
    assertThrows(UserNotFoundException.class, () -> userAuditService.readUserAudit(155L));
  }
}
