package com.tutoria.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static Properties properties = null;

    private static void carregarProperties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {

                if (input != null) {
                    properties.load(input);
                } else {
                    System.out.println("⚠️ Arquivo application.properties não encontrado. Usando apenas variáveis de ambiente.");
                }

            } catch (IOException e) {
                throw new RuntimeException("Erro ao carregar application.properties", e);
            }
        }
    }

    public static String getProperty(String chave) {
        carregarProperties();

        String envKey = chave.toUpperCase().replace('.', '_');
        String valor = System.getenv(envKey);

        if (valor == null) {
            valor = properties.getProperty(chave);
        }

        if (valor == null) {
            throw new RuntimeException("⚠️ Valor não encontrado para a chave: " + chave);
        }

        return valor;
    }

    public static String getDatabaseUrl() {
        return getProperty("db.url");
    }

    public static String getDatabaseUser() {
        return getProperty("db.user");
    }

    public static String getDatabasePassword() {
        return getProperty("db.password");
    }

    public static String getJwtSecret() {
        return getProperty("jwt.secret");
    }

    public static String getAIApiKey() {
        return getProperty("ai.api.key");
    }
}
