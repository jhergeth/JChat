package name.hergeth.jchat.debug;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record StatementView(
        String subject,
        String predicate,
        String object,
        String turnId,
        String source,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        Instant createdAt
) {}
