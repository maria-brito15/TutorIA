package com.tutoria.controller;

import java.sql.SQLException;
import java.util.Map;

import com.google.gson.Gson;
import com.tutoria.model.User;
import com.tutoria.service.UserService;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

public class UserController {

    private final UserService userService;
    private final Gson gson = new Gson();

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void configurarRotas() {
        post("/register", (req, res) -> {
            res.type("application/json");

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String nome = body.get("nome");
            String email = body.get("email");
            String senha = body.get("senha");

            if (nome == null || email == null || senha == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Todos os campos são obrigatórios"));
            }

            try {
                User existing = userService.buscarPorEmail(email);
                if (existing != null) {
                    res.status(409);
                    return gson.toJson(Map.of("error", "Email já cadastrado"));
                }

                User user = userService.registrarUsuario(nome, email, senha);

                return gson.toJson(Map.of(
                        "id", user.getId(),
                        "nome", user.getNome(),
                        "email", user.getEmail()
                ));
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Erro ao criar usuário"));
            }
        });

        get("/me", (req, res) -> {
            res.type("application/json");

            Integer userId = req.attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(Map.of("error", "Não autenticado"));
            }

            try {
                User user = userService.buscarPorId(userId);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Usuário não encontrado"));
                }

                return gson.toJson(Map.of(
                        "id", user.getId(),
                        "nome", user.getNome(),
                        "email", user.getEmail()
                ));
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Erro ao buscar usuário"));
            }
        });

        put("/me/nome", (req, res) -> {
            res.type("application/json");

            Integer userId = req.attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(Map.of("error", "Não autenticado"));
            }

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String novoNome = body.get("nome");

            if (novoNome == null || novoNome.isBlank()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Nome não informado"));
            }

            try {
                boolean atualizado = userService.atualizarNome(userId, novoNome);
                if (!atualizado) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Falha ao atualizar nome"));
                }

                User user = userService.buscarPorId(userId);
                return gson.toJson(Map.of(
                        "id", user.getId(),
                        "nome", user.getNome(),
                        "email", user.getEmail()
                ));
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Erro ao atualizar nome"));
            }
        });

        put("/me/senha", (req, res) -> {
            res.type("application/json");

            Integer userId = req.attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(Map.of("error", "Não autenticado"));
            }

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String novaSenha = body.get("senha");

            if (novaSenha == null || novaSenha.isBlank()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Senha não informada"));
            }

            try {
                boolean atualizado = userService.atualizarSenha(userId, novaSenha);
                if (!atualizado) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Falha ao atualizar senha"));
                }

                User user = userService.buscarPorId(userId);
                return gson.toJson(Map.of(
                        "id", user.getId(),
                        "nome", user.getNome(),
                        "email", user.getEmail()
                ));
            } catch (SQLException e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Erro ao atualizar senha"));
            }
        });
    }
}
