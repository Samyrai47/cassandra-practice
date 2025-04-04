package mipt.projectCassandra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageDto {
  private Long userId;
  private Action action;

  private MessageDto() {}
}
