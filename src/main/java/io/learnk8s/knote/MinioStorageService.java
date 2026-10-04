package io.learnk8s.knote;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioStorageService {
    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioProperties properties;
    private MinioClient client;

    public MinioStorageService(MinioProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        while (true) {
            try {
                MinioClient candidate = MinioClient.builder()
                        .endpoint("http://" + properties.getHost() + ":" + properties.getPort())
                        .credentials(properties.getAccessKey(), properties.getSecretKey())
                        .build();

                boolean bucketExists = candidate.bucketExists(
                        BucketExistsArgs.builder().bucket(properties.getBucket()).build());
                if (!bucketExists) {
                    candidate.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                    log.info("Created MinIO bucket '{}'.", properties.getBucket());
                }

                client = candidate;
                log.info("Connected to MinIO at {}:{}.", properties.getHost(), properties.getPort());
                return;
            } catch (Exception exception) {
                if (!properties.isReconnectEnabled()) {
                    throw new IllegalStateException("Could not connect to MinIO.", exception);
                }
                log.warn("MinIO is not ready; retrying in 5 seconds: {}", exception.getMessage());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for MinIO.", interrupted);
                }
            }
        }
    }

    public String save(MultipartFile file) throws Exception {
        String objectName = UUID.randomUUID().toString();
        String contentType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();

        try (InputStream input = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(input, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        }
        return objectName;
    }

    public StoredImage get(String objectName) throws Exception {
        var metadata = client.statObject(StatObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .build());
        try (InputStream input = client.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .build())) {
            return new StoredImage(input.readAllBytes(), metadata.contentType());
        }
    }

    public record StoredImage(byte[] bytes, String contentType) {
    }
}
