package br.com.presentya.service;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {
    private Slugs() {}

    /** "Acessórios para Celular" -> "acessorios-para-celular". */
    public static String de(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
