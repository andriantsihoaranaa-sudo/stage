package com.gestionnaissances.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Service cryptographique de gestion des mots de passe.
 *
 * Spécifications de sécurité :
 * - Algorithme standard : PBKDF2 avec HMAC-SHA256
 * - Sel cryptographique aléatoire : 16 octets (128 bits) généré par SecureRandom
 * - Nombre d'itérations : 65 536 (recommandations OWASP / NIST SP 800-132)
 * - Longueur de clé dérivée : 256 bits
 * - Format de stockage : "PBKDF2$65536$<sel_base64>$<hash_base64>"
 * - Comparaison à temps constant pour éliminer les attaques temporelles (timing attacks).
 * - Aucun mot de passe en clair n'est conservé ou affiché dans les logs.
 */
public class PasswordService {

    private static final String ALGORITHME = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int LONGUEUR_CLE_BITS = 256;
    private static final int LONGUEUR_SEL_OCTETS = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Génère une empreinte numérique sécurisée (hash + sel) pour un mot de passe en clair.
     *
     * @param motDePasseClair Le mot de passe saisi par l'utilisateur
     * @return La chaîne formatée "PBKDF2$65536$<sel>$<hash>"
     */
    public static String hasher(String motDePasseClair) {
        if (motDePasseClair == null || motDePasseClair.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut être vide ou nul.");
        }

        byte[] sel = new byte[LONGUEUR_SEL_OCTETS];
        RANDOM.nextBytes(sel);

        byte[] hash = deriverCle(motDePasseClair.toCharArray(), sel, ITERATIONS, LONGUEUR_CLE_BITS);

        String selB64 = Base64.getEncoder().encodeToString(sel);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        return "PBKDF2$" + ITERATIONS + "$" + selB64 + "$" + hashB64;
    }

    /**
     * Vérifie si un mot de passe en clair correspond à l'empreinte stockée.
     *
     * @param motDePasseCandidat Mot de passe soumis à la vérification
     * @param hashStocke         Hash enregistré en base de données
     * @return true si le mot de passe est valide, false sinon.
     */
    public static boolean verifier(String motDePasseCandidat, String hashStocke) {
        if (motDePasseCandidat == null || hashStocke == null || hashStocke.trim().isEmpty()) {
            return false;
        }

        if (hashStocke.startsWith("PBKDF2$")) {
            String[] segments = hashStocke.split("\\$");
            if (segments.length != 4) {
                return false;
            }

            try {
                int iters = Integer.parseInt(segments[1]);
                byte[] sel = Base64.getDecoder().decode(segments[2]);
                byte[] hashAttendu = Base64.getDecoder().decode(segments[3]);

                byte[] hashCalcule = deriverCle(motDePasseCandidat.toCharArray(), sel, iters, hashAttendu.length * 8);

                return slowEquals(hashAttendu, hashCalcule);
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private static byte[] deriverCle(char[] motDePasse, byte[] sel, int iterations, int longueurBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(motDePasse, sel, iterations, longueurBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHME);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Erreur cryptographique PBKDF2 : " + e.getMessage(), e);
        }
    }

    /**
     * Comparaison à temps constant pour prévenir les attaques par canal auxiliaire.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
