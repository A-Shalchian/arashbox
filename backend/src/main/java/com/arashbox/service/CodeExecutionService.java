package com.arashbox.service;

import com.arashbox.dto.ExecutionRequest;
import com.arashbox.dto.ExecutionResponse;
import com.arashbox.dto.OutputFrame;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    private final DockerClient dockerClient;

    @Value("${arashbox.execution.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${arashbox.execution.memory-limit-mb:128}")
    private int memoryLimitMb;

    private static final int MAX_OUTPUT_BYTES = 65_536;

    private static final Map<String, String> LANGUAGE_IMAGES = Map.of(
        "python", "python:3.12-slim",
        "javascript", "node:20-slim"
    );

    private static final Map<String, String> LANGUAGE_EXTENSIONS = Map.of(
        "python", "py",
        "javascript", "js"
    );

    private static final Map<String, String> LANGUAGE_INTERPRETERS = Map.of(
        "python", "python3",
        "javascript", "node"
    );

    public CodeExecutionService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public ExecutionResponse execute(ExecutionRequest request) {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        AtomicInteger exitCode = new AtomicInteger(1);
        long[] execTime = {0};

        executeStreaming(request.getCode(), request.getLanguage(), request.getStdin(), frame -> {
            switch (frame.getType()) {
                case "stdout" -> stdout.append(frame.getData());
                case "stderr" -> stderr.append(frame.getData());
                case "exit" -> {
                    exitCode.set(frame.getExitCode());
                    execTime[0] = frame.getExecutionTimeMs();
                }
                case "error" -> stderr.append(frame.getMessage());
            }
        });

        return new ExecutionResponse(stdout.toString(), stderr.toString(), exitCode.get(), execTime[0]);
    }

    public void executeStreaming(String code, String language, String stdin, Consumer<OutputFrame> frameConsumer) {
        String lang = language.toLowerCase();

        if (!LANGUAGE_IMAGES.containsKey(lang)) {
            frameConsumer.accept(OutputFrame.error("Unsupported language: " + lang));
            return;
        }

        String image = LANGUAGE_IMAGES.get(lang);
        String ext = LANGUAGE_EXTENSIONS.get(lang);
        String interpreter = LANGUAGE_INTERPRETERS.get(lang);

        String codeB64 = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));
        String stdinB64 = Base64.getEncoder().encodeToString(
                (stdin != null ? stdin : "").getBytes(StandardCharsets.UTF_8));

        String shellCmd = "printf '%s' \"$CODE_B64\" | base64 -d > /tmp/code." + ext
                + " && printf '%s' \"$STDIN_B64\" | base64 -d > /tmp/stdin.txt"
                + " && " + interpreter + " /tmp/code." + ext + " < /tmp/stdin.txt";

        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withEnv(List.of("CODE_B64=" + codeB64, "STDIN_B64=" + stdinB64,
                            "PYTHONUNBUFFERED=1"))
                    .withCmd("sh", "-c", shellCmd)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory((long) memoryLimitMb * 1024 * 1024)
                            .withCpuQuota(50000L)
                            .withNetworkMode("none")
                            .withReadonlyRootfs(true)
                            .withTmpFs(Map.of("/tmp", "rw,noexec,size=10m"))
                            .withPidsLimit(16L)
                            .withCapDrop(Capability.ALL)
                    )
                    .withUser("nobody")
                    .withTty(false)
                    .exec();

            containerId = container.getId();

            dockerClient.startContainerCmd(containerId).exec();

            AtomicInteger totalBytes = new AtomicInteger(0);

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback<Frame>() {
                        @Override public void onStart(Closeable closeable) {}

                        @Override
                        public void onNext(Frame frame) {
                            if (totalBytes.get() >= MAX_OUTPUT_BYTES) return;
                            String payload = new String(frame.getPayload());
                            totalBytes.addAndGet(payload.length());
                            switch (frame.getStreamType()) {
                                case STDOUT -> frameConsumer.accept(OutputFrame.stdout(payload));
                                case STDERR -> frameConsumer.accept(OutputFrame.stderr(payload));
                                default -> {}
                            }
                        }

                        @Override public void onError(Throwable throwable) {}
                        @Override public void onComplete() {}
                        @Override public void close() {}
                    });

            int exit = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS);

            long executionTime = System.currentTimeMillis() - startTime;

            if (totalBytes.get() >= MAX_OUTPUT_BYTES) {
                frameConsumer.accept(OutputFrame.stderr("\n... output truncated (64KB limit)"));
            }

            frameConsumer.accept(OutputFrame.exit(exit, executionTime));

        } catch (Exception e) {
            log.error("Code execution failed", e);
            long executionTime = System.currentTimeMillis() - startTime;
            frameConsumer.accept(OutputFrame.error("Execution failed: " + e.getMessage()));
            frameConsumer.accept(OutputFrame.exit(1, executionTime));
        } finally {
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
