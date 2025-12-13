package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class Issue10365Test {

    @Test
    public void testPostHookExecutionOnStop() {
        String scriptContent =
            "#!/bin/bash\n" +
            "function on_shutdown() {\n" +
            "  echo 'HOOK_TRIGGERED'\n" +
            "}\n" +
            "trap on_shutdown SIGTERM SIGINT EXIT\n" +
            "echo 'CONTAINER_STARTED'\n" +
            "while true; do sleep 1; done\n";

        try (
            GenericContainer<?> container = new GenericContainer<>(
                new ImageFromDockerfile()
                    .withFileFromString("entrypoint.sh", scriptContent)
                    .withDockerfileFromBuilder(builder ->
                        builder
                            .from("alpine:3.18")
                            .run("apk add --no-cache tini bash")
                            .copy("entrypoint.sh", "/entrypoint.sh")
                            .run("chmod +x /entrypoint.sh")
                            .entryPoint("/sbin/tini", "--", "/entrypoint.sh")
                            .build()
                    )
            )
        ) {
            ToStringConsumer logConsumer = new ToStringConsumer();
            container.withLogConsumer(logConsumer);
            
            container.withStartupTimeout(Duration.ofSeconds(30));

            container.start();

            container.stop();

            String logs = logConsumer.toUtf8String();
            System.out.println("=== CONTAINER LOGS ===\n" + logs + "\n======================");

            assertThat(logs)
                .contains("HOOK_TRIGGERED");
        }
    }
}