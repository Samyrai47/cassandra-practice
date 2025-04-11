package mipt.projectCassandra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageDto {
  private long userId;
  private Action action;

  public MessageDto() {}
}
