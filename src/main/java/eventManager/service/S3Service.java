package eventManager.service;

/**
 * Servicio para gestionar operaciones de almacenamiento de imágenes.
 */
public interface S3Service {
    
    /**
     * Sube una imagen al almacenamiento.
     * @param imageBytes Bytes de la imagen
     * @param fileName Nombre del archivo
     * @param contentType Tipo de contenido
     * @return Clave donde se guardó la imagen
     */
    String uploadImage(byte[] imageBytes, String fileName, String contentType);
    
    /**
     * Genera una URL firmada para acceso temporal.
     * @param s3Key Clave de la imagen
     * @return URL firmada válida por 7 días
     */
    String generatePresignedUrl(String s3Key);
    
    /**
     * Elimina una imagen del almacenamiento.
     * @param s3Key Clave de la imagen
     */
    void deleteImage(String s3Key);
}
