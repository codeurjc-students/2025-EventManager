package eventManager.service;

/**
 * Servicio para gestionar operaciones con AWS S3
 * Este servicio maneja la carga, descarga y eliminación de imágenes en S3
 */
public interface S3Service {
    
    /**
     * Sube una imagen a S3
     * @param imageBytes Bytes de la imagen
     * @param fileName Nombre del archivo
     * @param contentType Tipo de contenido (image/jpeg, image/png, etc.)
     * @return S3 key (ruta) donde se guardó la imagen
     */
    String uploadImage(byte[] imageBytes, String fileName, String contentType);
    
    /**
     * Genera una URL firmada (presigned URL) para acceder temporalmente a una imagen
     * @param s3Key Clave/ruta de la imagen en S3
     * @return URL firmada válida por 7 días
     */
    String generatePresignedUrl(String s3Key);
    
    /**
     * Elimina una imagen de S3
     * @param s3Key Clave/ruta de la imagen en S3
     */
    void deleteImage(String s3Key);
}
