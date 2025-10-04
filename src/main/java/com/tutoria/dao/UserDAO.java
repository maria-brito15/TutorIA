package com.tutoria.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mindrot.jbcrypt.BCrypt;

import com.tutoria.model.User;

public class UserDAO {

    private final Connection conn;

    public UserDAO(Connection conn) {
        this.conn = conn;
    }

    public void inicializarTabela() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id SERIAL PRIMARY KEY,
                nome VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                senha VARCHAR(200) NOT NULL
            )
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public User criar(User user) throws SQLException {
        String senhaHash = BCrypt.hashpw(user.getSenha(), BCrypt.gensalt());
        String sql = "INSERT INTO usuarios (nome, email, senha) VALUES (?, ?, ?) RETURNING id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getNome());
            ps.setString(2, user.getEmail());
            ps.setString(3, senhaHash);

            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                return new User(id, user.getNome(), user.getEmail(), senhaHash);
            }
        }

        throw new SQLException("Erro ao criar usuário");
    }

    public User buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT id, nome, email, senha FROM usuarios WHERE email = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha"));
            }
        }

        return null;
    }

    public User buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nome, email, senha FROM usuarios WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha"));
            }
        }

        return null;
    }

    public boolean atualizarNome(int id, String novoNome) throws SQLException {
        String sql = "UPDATE usuarios SET nome = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean atualizarSenha(int id, String novaSenha) throws SQLException {
        String hash = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public User autenticar(String email, String senha) throws SQLException {
        User user = buscarPorEmail(email);

        if (user != null && BCrypt.checkpw(senha, user.getSenha())) {
            return user;
        }

        return null;
    }
}
