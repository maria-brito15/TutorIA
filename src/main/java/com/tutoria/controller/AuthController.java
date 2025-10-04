package com.tutoria.controller;

import com.tutoria.model.User;
import com.tutoria.service.UserService;
import com.tutoria.service.AuthService;
import com.google.gson.Gson;

import static spark.Spark.post;

import java.util.Map;

public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final Gson gson = new Gson();

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    public void configurarRotas() {
        post("/login", (req, res) -> {
            var body = gson.fromJson(req.body(), Map.class);
            
            String email = (String) body.get("email");
            String senha = (String) body.get("senha");

            User u = userService.autenticarUsuario(email, senha);
            if (u == null) {
                res.status(401);
                return gson.toJson(Map.of("error", "Credenciais inválidas"));
            }

            String token = authService.gerarToken(u.getId(), u.getEmail());
            return gson.toJson(Map.of("token", token));
        });

        post("/logout", (req, res) -> {
            res.type("application/json");

            String authHeader = req.headers("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.status(401);
                return gson.toJson(Map.of("error", "Token não fornecido"));
            }

            return gson.toJson(Map.of(
                "mensagem", "Logout realizado com sucesso."
            ));
        });
    }
}
