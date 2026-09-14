package com.gestionnaissances.security;

/**
 * Rôles applicatifs officiels pour la gestion des naissances :
 * 1. ADMINISTRATEUR : accès intégral (gestion dossiers, statistiques, utilisateurs, configuration)
 * 2. RESPONSABLE_COMMUNAL : validation, consultation, statistiques, modification des dossiers
 * 3. AGENT : saisie des naissances, recherche, consultation des fiches et documents
 */
public enum Role {
    ADMINISTRATEUR("Administrateur", "Accès complet au système et gestion des comptes"),
    RESPONSABLE_COMMUNAL("Responsable Communal", "Supervision, validation et statistiques"),
    AGENT("Agent d'État Civil", "Saisie des actes et consultations autorisées");

    private final String libelle;
    private final String description;

    Role(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convertit une chaîne de caractères en Role de façon tolérante et sécurisée.
     */
    public static Role depuisChaine(String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return AGENT;
        }
        String v = valeur.trim().toUpperCase();
        switch (v) {
            case "ADMIN":
            case "ADMINISTRATEUR":
                return ADMINISTRATEUR;
            case "RESPONSABLE":
            case "RESPONSABLE_COMMUNAL":
                return RESPONSABLE_COMMUNAL;
            case "AGENT":
            default:
                return AGENT;
        }
    }
}
