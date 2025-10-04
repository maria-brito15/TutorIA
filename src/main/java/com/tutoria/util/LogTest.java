package com.tutoria.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {
    private static final Logger logger = LoggerFactory.getLogger(LogTest.class);

    public static void main(String[] args) {
        logger.info("Se você está vendo esta mensagem, o SLF4J + Logback está funcionando!");
        logger.debug("Mensagem de DEBUG (pode aparecer ou não dependendo da config).");
        logger.error("Mensagem de ERRO!");
    }
}
