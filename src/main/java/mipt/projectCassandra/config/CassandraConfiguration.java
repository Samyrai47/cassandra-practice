package mipt.projectCassandra.config;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import java.net.InetSocketAddress;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("spring.data.cassandra")
@Setter
@Getter
public class CassandraConfiguration {
  private String contactPoints;
  private int port;
  private String keyspaceName;
  private String localDatacenter;
  private CqlSession session;

  @Bean
  public CqlSession cqlSession(CqlSessionBuilder sessionBuilder) {
    InetSocketAddress address = InetSocketAddress.createUnresolved(contactPoints, port);
    sessionBuilder = sessionBuilder.addContactPoint(address);
    sessionBuilder.withKeyspace((CqlIdentifier) null).withLocalDatacenter(localDatacenter);

    CqlSession session = sessionBuilder.build();

    SimpleStatement statement =
        SchemaBuilder.createKeyspace(keyspaceName)
            .ifNotExists()
            .withNetworkTopologyStrategy(Map.of(localDatacenter, 1))
            .build();
    session.execute(statement);

    session.execute(
        String.format(
            """
            CREATE TABLE IF NOT EXISTS %s
            .user_audit (user_id BIGINT, event_time TIMESTAMP, event_type TEXT, event_details TEXT, PRIMARY KEY ((user_id), event_time))
            WITH CLUSTERING ORDER BY (event_time DESC) AND default_time_to_live = 31536000;
            """,
            keyspaceName));

    return sessionBuilder.withKeyspace(keyspaceName).build();
  }
}
