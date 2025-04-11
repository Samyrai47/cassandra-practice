package mipt.projectCassandra.entity;

import java.time.Instant;

public record UserAudit(Long userId, Instant time, String description) {}
