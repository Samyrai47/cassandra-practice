package mipt.projectCassandra.entity;

import java.time.Instant;
import java.util.UUID;

public record UserAudit(UUID userId, Instant time, String description) {}
