package eventManager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Configurar el manejo de recursos estáticos para SPA con Vue Router
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // Si el recurso existe, devolverlo
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Si no es una ruta de API y el recurso no existe, devolver index.html para Vue Router
                        if (!isApiRoute(resourcePath)) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                    
                    private boolean isApiRoute(String resourcePath) {
                        return resourcePath.startsWith("api/") ||
                               resourcePath.startsWith("autenticacion/") ||
                               resourcePath.startsWith("usuario/") ||
                               resourcePath.startsWith("eventos/") ||
                               resourcePath.startsWith("regalo/") ||
                               resourcePath.startsWith("entrada/") ||
                               resourcePath.startsWith("actuator/") ||
                               resourcePath.startsWith("swagger-ui/") ||
                               resourcePath.startsWith("v3/api-docs/");
                    }
                });
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // Configuración CORS para permitir el frontend en desarrollo
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
