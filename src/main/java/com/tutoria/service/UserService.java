package com.tutoria.service;

import com.tutoria.dao.UserDAO;
import com.tutoria.model.User;

import java.sql.Connection;
import java.sql.SQLException;

public class UserService {

    private final UserDAO userDAO;

    public UserService(Connection conn) {
        this.userDAO = new UserDAO(conn);
    }

    public void inicializarTabela() throws SQLException {
        userDAO.inicializarTabela();
    }

    public User registrarUsuario(String nome, String email, String senha) throws SQLException {
        return userDAO.criar(new User(nome, email, senha));
    }

    public User autenticarUsuario(String email, String senha) throws SQLException {
        return userDAO.autenticar(email, senha);
    }

    public User buscarPorId(int id) throws SQLException {
        return userDAO.buscarPorId(id);
    }

    public User buscarPorEmail(String email) throws SQLException {
        return userDAO.buscarPorEmail(email);
    }

    public boolean atualizarNome(int id, String novoNome) throws SQLException {
        return userDAO.atualizarNome(id, novoNome);
    }

    public boolean atualizarSenha(int id, String novaSenha) throws SQLException {
        return userDAO.atualizarSenha(id, novaSenha);
    }

    public boolean deletarUsuario(int id) throws SQLException {
        return userDAO.deletar(id);
    }
}
