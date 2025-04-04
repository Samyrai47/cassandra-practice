package mipt.projectCassandra.service;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import mipt.projectCassandra.dto.Action;
import mipt.projectCassandra.dto.MessageDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {KafkaConsumerService.class},
    properties = {
      "topic-to-consume-message=test-topic",
      "spring.kafka.consumer.group-id=some-consumer-group",
      "spring.kafka.consumer.auto-offset-reset=earliest"
    })
@Import({KafkaAutoConfiguration.class, KafkaConsumerServiceTest.ObjectMapperTestConfig.class})
@Testcontainers
class KafkaConsumerServiceTest {

  @TestConfiguration
  static class ObjectMapperTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @Container @ServiceConnection
  public static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

  @MockitoBean private UserAuditService userAuditService;

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldSendMessageToKafkaSuccessfully()
      throws JsonProcessingException, ExecutionException, InterruptedException {
    MessageDto testMessage = new MessageDto(1L, Action.DELETE);
    String messageJson = objectMapper.writeValueAsString(testMessage);

    kafkaTemplate.send("test-topic", messageJson).get();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () ->
                Mockito.verify(userAuditService, times(1))
                    .createEventAudit(eq(testMessage.getUserId()), eq(testMessage.getAction())));
  }

  @Test
  void shouldNotCallAudit()
      throws JsonProcessingException, ExecutionException, InterruptedException {
    MessageDto testMessage = new MessageDto(12L, Action.INSERT);
    String messageJson = objectMapper.writeValueAsString(testMessage);

    kafkaTemplate.send("wrong-topic", messageJson).get();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () ->
                Mockito.verify(userAuditService, times(0))
                    .createEventAudit(eq(testMessage.getUserId()), eq(testMessage.getAction())));
  }
}
