package mipt.projectCassandra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CountDownLatch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mipt.projectCassandra.dto.MessageDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
  private final ObjectMapper objectMapper;
  private final UserAuditService auditService;
  @Getter private CountDownLatch latch = new CountDownLatch(1);

  @KafkaListener(topics = {"${topic-to-consume-message}"})
  public void consumeMessage(String message) throws JsonProcessingException {
    JsonNode rootNode = objectMapper.readTree(message);
    MessageDto parsedMessage = objectMapper.treeToValue(rootNode.get("value"), MessageDto.class);
    log.info("Retrieved message {}", message);
    latch.countDown();
    auditService.createEventAudit(parsedMessage.getUserId(), parsedMessage.getAction());
  }
}
