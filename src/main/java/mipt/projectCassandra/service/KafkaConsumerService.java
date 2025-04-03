package mipt.projectCassandra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  @KafkaListener(topics = {"${topic-to-consume-message}"})
  public void consumeMessage(String message) throws JsonProcessingException {
    MessageDto parsedMessage = objectMapper.readValue(message, MessageDto.class);
    log.info("Retrieved message {}", message);
    auditService.createEventAudit(parsedMessage.getUserId(), parsedMessage.getAction());
  }
}
