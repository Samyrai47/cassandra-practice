package mipt.projectCassandra.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import mipt.projectCassandra.dto.Action;
import mipt.projectCassandra.dto.MessageDto;
import org.junit.jupiter.api.Test;
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
      "topic-to-produce-message=test-topic",
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

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private KafkaConsumerService consumerService;
  @MockitoBean private UserAuditService auditService;

  @Test
  void shouldSendMessageToKafkaSuccessfully()
      throws JsonProcessingException, ExecutionException, InterruptedException {
    MessageDto testMessage = new MessageDto(1L, Action.DELETE);
    Map<String, MessageDto> testMap = new HashMap<>();
    testMap.put("value", testMessage);
    String messageJson = objectMapper.writeValueAsString(testMap);

    kafkaTemplate.send("test-topic", messageJson).get();

    doNothing()
        .when(auditService)
        .createEventAudit(eq(testMessage.getUserId()), eq(testMessage.getAction()));

    boolean messageConsumed = consumerService.getLatch().await(10, TimeUnit.SECONDS);
    assertTrue(messageConsumed);
  }

  @Test
  void shouldFailToFindMessage() throws JsonProcessingException, InterruptedException {
    MessageDto testMessage = new MessageDto(2L, Action.INSERT);
    Map<String, MessageDto> testMap = new HashMap<>();
    testMap.put("value", testMessage);
    String messageJson = objectMapper.writeValueAsString(testMap);
    kafkaTemplate.send("wrong-topic", messageJson);

    boolean messageConsumed = consumerService.getLatch().await(4, TimeUnit.SECONDS);
    assertFalse(messageConsumed);
  }
}
