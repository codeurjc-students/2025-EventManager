package eventManager.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración del cliente MinIO para desarrollo local.
 * Solo se activa con el perfil 'dev'.
 * El bucket se crea automáticamente si no existe.
 */
@Configuration
@Profile("dev")
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public AmazonS3 minioClient() {
        log.info("Configurando cliente MinIO para desarrollo...");
        log.info("Endpoint: {}", endpoint);
        log.info("Bucket: {}", bucketName);

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        endpoint, 
                        "us-east-1" // Region no importa para MinIO, pero es requerido
                ))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true) // Importante para MinIO
                .build();

        // Crear bucket si no existe
        try {
            if (!s3Client.doesBucketExistV2(bucketName)) {
                log.info("Creando bucket: {}", bucketName);
                s3Client.createBucket(bucketName);
                log.info("Bucket creado exitosamente");
            } else {
                log.info("Bucket existente: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error al verificar/crear bucket: {}", e.getMessage());
        }

        return s3Client;
    }
}
