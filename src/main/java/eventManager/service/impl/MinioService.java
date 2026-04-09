package eventManager.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.HttpMethod;
import eventManager.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * Implementación del servicio S3 usando MinIO para desarrollo local.
 * Solo se activa con el perfil 'dev'.
 */
@Service
@Profile("dev")
@Slf4j
public class MinioService implements S3Service {

    @Autowired
    private AmazonS3 minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public String uploadImage(byte[] imageBytes, String fileName, String contentType) {
        try {
            // Generar nombre único para evitar colisiones
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            String s3Key = "gifts/" + uniqueFileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);
            metadata.setContentType(contentType);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, s3Key, inputStream, metadata);

            minioClient.putObject(putRequest);
            
            log.info("Imagen subida a MinIO: {}", s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Error al subir imagen a MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir imagen a MinIO", e);
        }
    }

    @Override
    public String generatePresignedUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return null;
        }
        try {
            // URL firmada válida por 7 días
            Date expiration = new Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
            URL url = minioClient.generatePresignedUrl(bucketName, s3Key, expiration, HttpMethod.GET);
            
            // Para desarrollo local, podemos usar la URL directa de MinIO
            // La URL firmada funcionará, pero también podemos hacer el objeto público
            log.debug("Presigned URL generada: {}", url.toString());
            return url.toString();
        } catch (Exception e) {
            log.error("Error al generar presigned URL en MinIO: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void deleteImage(String s3Key) {
        String normalizedKey = normalizeS3Key(s3Key);
        if (normalizedKey == null || normalizedKey.isEmpty()) {
            return;
        }
        try {
            minioClient.deleteObject(bucketName, normalizedKey);
            log.info("Imagen eliminada de MinIO: {}", normalizedKey);
        } catch (Exception e) {
            log.error("Error al eliminar imagen de MinIO: {}", e.getMessage(), e);
        }
    }

    private String normalizeS3Key(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isEmpty()) {
            return null;
        }
        if (!s3KeyOrUrl.startsWith("http")) {
            return s3KeyOrUrl;
        }
        try {
            URI uri = URI.create(s3KeyOrUrl);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return s3KeyOrUrl;
            }
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            if (cleanPath.startsWith(bucketName + "/")) {
                cleanPath = cleanPath.substring(bucketName.length() + 1);
            }
            return cleanPath;
        } catch (Exception e) {
            return s3KeyOrUrl;
        }
    }
}
