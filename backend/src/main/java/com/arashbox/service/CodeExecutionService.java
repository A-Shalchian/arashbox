package com.arashbox.service;

import com.arashbox.dto.ExecutionRequest;
import com.arashbox.dto.ExecutionResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    private final DockerClient dockerClient;

    @Value("${arashbox.execution.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${arashbox.execution.memory-limit-mb:128}")
    private int memoryLimitMb;

    private static final Map<String, String> LANGUAGE_IMAGES = Map.of(
        "python", "python:3.12-slim",
        "javascript", "node:20-slim"
    );

    private static final Map<String, String[]> LANGUAGE_COMMANDS = Map.of(
        "python", new String[]{"python3", "-c"},
        "javascript", new String[]{"node", "-e"}
    );

    public CodeExecutionService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public ExecutionResponse execute(ExecutionRequest request) {
        String language = request.getLanguage().toLowerCase();

        if (!LANGUAGE_IMAGES.containsKey(language)) {
            return new ExecutionResponse("", "Unsupported language: " + language, 1, 0);
        }

        String image = LANGUAGE_IMAGES.get(language);
        String[] baseCmd = LANGUAGE_COMMANDS.get(language);
        String[] cmd = new String[]{baseCmd[0], baseCmd[1], request.getCode()};

        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            // Create container with resource limits
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withCmd(cmd)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory((long) memoryLimitMb * 1024 * 1024)
                            .withCpuQuota(50000L) // 0.5 CPU
                            .withNetworkMode("none") // No network access
                            .withReadonlyRootfs(false)
                    )
                    .withTty(false)
                    .exec();

            containerId = container.getId();

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Collect output
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback<Frame>() {
                        @Override
                        public void onStart(Closeable closeable) {}

                        @Override
                        public void onNext(Frame frame) {
                            String payload = new String(frame.getPayload());
                            switch (frame.getStreamType()) {
                                case STDOUT -> stdout.append(payload);
                                case STDERR -> stderr.append(payload);
                                default -> {}
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {}

                        @Override
                        public void onComplete() {}

                        @Override
                        public void close() {}
                    });

            // Wait for container to finish
            int exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);

            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResponse(stdout.toString(), stderr.toString(), exitCode, executionTime);

        } catch (Exception e) {
            log.error("Code execution failed", e);
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResponse("", "Execution failed: " + e.getMessage(), 1, executionTime);
        } finally {
            // Always clean up the container
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.warn("Failed to remove container: {}", containerId, e);
                }
            }
        }
    }
}
