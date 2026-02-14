package eventManager.config;

//import com.amazonaws.auth.InstanceProfileCredentialsProvider;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración del cliente AWS S3
 * Solo se activa cuando el perfil 'aws' está activo
 * 
 * INSTRUCCIONES PARA ACTIVAR:
 * 1. Descomentar todas las importaciones y el código del @Bean
 * 2. Asegurar que la EC2 tiene un IAM Role con permisos:
 *    - s3:PutObject
 *    - s3:GetObject
 *    - s3:DeleteObject
 * 3. Configurar la propiedad aws.s3.region en application.yml
 * 4. Activar el perfil: -Dspring.profiles.active=aws
 * 
 * SEGURIDAD: Usa InstanceProfileCredentialsProvider para obtener credenciales
 * del IAM Role de la instancia EC2 (no hardcodear credenciales)
 */
@Configuration
@Profile("aws")
public class S3Config {

    /*
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
    */
}
