package mipt.projectCassandra.config;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cassandra.DriverConfigLoaderBuilderCustomizer;
import org.springframework.stereotype.Component;

@Component
public class CassandraDriverConfigLoaderBuilderCustomizer
    implements DriverConfigLoaderBuilderCustomizer {

  @Override
  public void customize(
      ProgrammaticDriverConfigLoaderBuilder programmaticDriverConfigLoaderBuilder) {
    programmaticDriverConfigLoaderBuilder
        .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(10))
        .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(10))
        .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10));
  }
}
