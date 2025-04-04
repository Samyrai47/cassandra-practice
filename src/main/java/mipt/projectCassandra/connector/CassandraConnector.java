package mipt.projectCassandra.connector;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import java.net.InetSocketAddress;
import lombok.Getter;

@Getter
public class CassandraConnector {
  private CqlSession session;

  public void connect(String node, Integer port, String dataCenter) {
    CqlSessionBuilder builder = CqlSession.builder();
    builder.addContactPoint(new InetSocketAddress(node, port));
    builder.withLocalDatacenter(dataCenter);
    session = builder.build();
  }

  public void close() {
    session.close();
  }
}
