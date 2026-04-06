package bobby.detectors;

import java.util.concurrent.CompletableFuture;

public interface Detector {
    String getName();
    CompletableFuture<Void> scan();
    void stop();
}
