package eventManager.config;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración del cliente AWS S3 para producción.
 * Solo se activa con el perfil 'aws'.
 * Usa credenciales del IAM Role de la instancia EC2.
 */
@Configuration
@Profile("aws")
public class S3Config {

    @Value("${aws.s3.region:eu-west-1}")
    private String region;

    @Bean
    public AmazonS3 s3Client() {
        // Usar IAM Role de EC2 (recomendado en producción)
        // No requiere credenciales explícitas, las obtiene automáticamente del role
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .build();
    }
}

