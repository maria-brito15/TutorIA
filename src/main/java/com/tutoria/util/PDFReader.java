package com.tutoria.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PDFReader {

    public String extrairTextoDePDF(InputStream inputStream) throws IOException {
        PDDocument documento = null;
        String textoExtraido = "";

        try {
            documento = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            textoExtraido = stripper.getText(documento);

        } finally {
            if (documento != null) {
                documento.close();
            }
        }

        return textoExtraido;
    }

    public String extrairTextoDePDF(File arquivoPDF) throws IOException {
        PDDocument documento = null;
        String textoExtraido = "";

        try {
            documento = PDDocument.load(arquivoPDF);
            PDFTextStripper stripper = new PDFTextStripper();
            textoExtraido = stripper.getText(documento);

        } finally {
            if (documento != null) {
                documento.close();
            }
        }

        return textoExtraido;
    }
}
