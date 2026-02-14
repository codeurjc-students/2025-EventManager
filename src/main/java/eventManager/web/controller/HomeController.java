package eventManager.web.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    // Catch all routes that don't match API endpoints and forward to index.html for SPA routing
    @GetMapping({"/iniciar-sesion", "/registro", "/eventos/**", "/usuario/**", "/regalo/**"})
    public String spaRoutes() {
        return "forward:/index.html";
    }

    // Endpoint de prueba para verificar que el backend funciona
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "Backend is working correctly",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    // Página de prueba para diagnosticar problemas
    @GetMapping("/test")
    public String testPage() {
        return "forward:/test.html";
    }
}