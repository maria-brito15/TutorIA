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

        // 1. Tenta pegar do ambiente primeiro
        String envKey = chave.toUpperCase().replace('.', '_');
        String valor = System.getenv(envKey);

        // 2. Se não existir, pega do properties
        if (valor == null) {
            valor = properties.getProperty(chave);

            // 3. Substitui ${VAR} pelo valor da variável de ambiente
            if (valor != null) {
                valor = substituirVariavelAmbiente(valor);
            }
        }

        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("⚠️ Valor não encontrado para a chave: " + chave);
        }

        return valor;
    }

    private static String substituirVariavelAmbiente(String valor) {
        // Procura por padrões do tipo ${VAR}
        while (valor.contains("${") && valor.contains("}")) {
            int inicio = valor.indexOf("${");
            int fim = valor.indexOf("}", inicio);
            if (fim > inicio) {
                String varName = valor.substring(inicio + 2, fim);
                String envValue = System.getenv(varName);
                if (envValue == null) {
                    envValue = ""; // ou lançar exceção, se preferir
                }
                valor = valor.substring(0, inicio) + envValue + valor.substring(fim + 1);
            } else {
                break;
            }
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
