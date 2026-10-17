package com.miniframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuration de l'application (fichier .properties du classpath de la webapp).
 * Chargée UNE fois par le FrontController dans init(), puis en lecture seule :
 * pas d'état mutable partagé entre les threads (même règle que pour les contrôleurs).
 */
public final class AppConfig {

    // volatile : les threads de requêtes voient la valeur publiée par init()
    private static volatile Properties properties;

    private AppConfig() {}

    /** Charge le fichier depuis le classpath de la webapp (WEB-INF/classes). */
    public static void load(String resourceName) throws IOException {
        // Même ClassLoader que le scanner : celui de Tomcat voit WEB-INF/classes
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Fichier de configuration introuvable : " + resourceName
                        + " (attendu dans WEB-INF/classes)");
            }
            Properties p = new Properties();
            // Reader UTF-8 : load(InputStream) lirait en ISO-8859-1 et casserait les accents
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(reader);
            }
            properties = p; // publié seulement une fois entièrement chargé
        }
    }

    /** Valeur obligatoire : clé absente = erreur claire. */
    public static String get(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new IllegalStateException("Clé de configuration manquante : '" + key + "'");
        }
        return value;
    }

    /** Valeur avec défaut si la clé est absente. */
    public static String get(String key, String defaultValue) {
        Properties p = properties;
        if (p == null) {
            throw new IllegalStateException(
                    "AppConfig n'est pas chargée (AppConfig.load doit être appelée dans FrontController.init()).");
        }
        String value = p.getProperty(key);
        // trim : un espace oublié en fin de ligne fait partie de la valeur dans un .properties
        return value == null ? defaultValue : value.trim();
    }
}
