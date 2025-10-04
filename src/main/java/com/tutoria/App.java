package com.tutoria;

import com.tutoria.controller.AIController;
import com.tutoria.controller.AuthController;
import com.tutoria.controller.UserController;
import com.tutoria.service.AIService;
import com.tutoria.service.AuthService;
import com.tutoria.service.UserService;
import com.tutoria.util.ConfigLoader;

import static spark.Spark.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

public class App {

    private static final int PORT = 4567;
    private static final Set<String> ROTAS_PUBLICAS = Set.of("/health", "/login", "/register", "/");

    public static void main(String[] args) throws SQLException {

        // Conexão com banco
        Connection conn = DriverManager.getConnection(
                ConfigLoader.getDatabaseUrl(),
                ConfigLoader.getDatabaseUser(),
                ConfigLoader.getDatabasePassword()
        );

        UserService userService = new UserService(conn);
        userService.inicializarTabela();

        AuthService authService = new AuthService();
        AIService aiService = new AIService(ConfigLoader.getAIApiKey());

        port(PORT);

        // Frontend
        staticFiles.location("/public");

        configurarCORS(authService);

        // 🔒 Filtro global de autenticação
        before((req, res) -> {
            String path = req.pathInfo();

            // Libera preflight de CORS
            if (req.requestMethod().equals("OPTIONS")) {
                return;
            }

            // Libera rotas públicas da API
            if (ROTAS_PUBLICAS.contains(path)) {
                return;
            }

            // Libera arquivos estáticos (html, js, css, imagens, etc.)
            if (path.matches(".*\\.[a-zA-Z0-9]+$")) {
                return;
            }

            // 🔒 Verificação do token
            String authHeader = req.headers("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                halt(401, "{\"error\":\"Token não fornecido\"}");
            }

            String token = authHeader.substring(7);
            try {
                int userId = authService.getUserId(token);
                req.attribute("userId", userId);
            } catch (Exception e) {
                halt(401, "{\"error\":\"Token inválido ou expirado\"}");
            }
        });

        // Configuração das rotas
        new AIController(aiService).configurarRotas();
        new UserController(userService).configurarRotas();
        new AuthController(userService, authService).configurarRotas();

        // Rotas extras
        get("/teste", (req, res) -> {
            res.type("application/json");
            return "{\"mensagem\": \"TutorIA API está rodando!\", \"versao\": \"1.0.0\"}";
        });

        get("/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\": \"ok\"}";
        });

        // Exemplo de rotas protegidas específicas
        protegerRotas(authService, "/ai/*", "/me/nome", "/me/senha", "/me");
    }

    private static void configurarCORS(AuthService authService) {
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) res.header("Access-Control-Allow-Headers", headers);

            String methods = req.headers("Access-Control-Request-Method");
            if (methods != null) res.header("Access-Control-Allow-Methods", methods);

            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
    }

    @SafeVarargs
    private static void protegerRotas(AuthService authService, String... paths) {
        for (String path : paths) {
            before(path, (req, res) -> {
                String authHeader = req.headers("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    halt(401, "{\"error\":\"Token não fornecido\"}");
                }

                String token = authHeader.substring(7);
                try {
                    int userId = authService.getUserId(token);
                    req.attribute("userId", userId);
                } catch (Exception e) {
                    halt(401, "{\"error\":\"Token inválido ou expirado\"}");
                }
            });
        }
    }
}
