package mipt.projectCassandra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
}
