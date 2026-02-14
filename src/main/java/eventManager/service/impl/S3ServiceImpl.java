package eventManager.service.impl;

//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.amazonaws.HttpMethod;
import eventManager.service.S3Service;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//import java.io.ByteArrayInputStream;
//import java.net.URL;
//import java.util.Date;
//import java.util.UUID;

/**
 * Implementación del servicio S3 para AWS
 * Solo se activa cuando el perfil 'aws' está activo
 * 
 * INSTRUCCIONES PARA ACTIVAR:
 * 1. Descomentar todas las importaciones y el código
 * 2. Configurar las propiedades aws.s3.* en application.yml
 * 3. Asegurar que la instancia EC2 tiene IAM Role con permisos S3
 * 4. Activar el perfil: -Dspring.profiles.active=aws
 */
@Service
@Profile("aws") // Solo se activa con profile aws
@Slf4j
public class S3ServiceImpl implements S3Service {

    /*
    @Autowired
    private AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region:eu-west-1}")
    private String region;

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

            s3Client.putObject(putRequest);
            
            log.info("Imagen subida a S3: {}", s3Key);
            return s3Key; // Devolver la key (ruta en S3)
        } catch (Exception e) {
            log.error("Error al subir imagen a S3: {}", e.getMessage());
            throw new RuntimeException("Error al subir imagen a S3", e);
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
            URL url = s3Client.generatePresignedUrl(bucketName, s3Key, expiration, HttpMethod.GET);
            return url.toString();
        } catch (Exception e) {
            log.error("Error al generar presigned URL: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteImage(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return;
        }
        try {
            s3Client.deleteObject(bucketName, s3Key);
            log.info("Imagen eliminada de S3: {}", s3Key);
        } catch (Exception e) {
            log.error("Error al eliminar imagen de S3: {}", e.getMessage());
        }
    }
    */

    // Implementación dummy para desarrollo local (profile dev)
    // Estos métodos nunca se ejecutarán en dev porque el bean no se crea
    @Override
    public String uploadImage(byte[] imageBytes, String fileName, String contentType) {
        log.warn("S3Service no está disponible en perfil dev");
        return null;
    }

    @Override
    public String generatePresignedUrl(String s3Key) {
        log.warn("S3Service no está disponible en perfil dev");
        return null;
    }

    @Override
    public void deleteImage(String s3Key) {
        log.warn("S3Service no está disponible en perfil dev");
    }
}
