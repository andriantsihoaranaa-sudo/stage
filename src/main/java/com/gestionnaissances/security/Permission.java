package com.gestionnaissances.security;

/**
 * Permissions granulaires de l'application « Gestion des Naissances ».
 * Définies selon la matrice de sécurité de l'Étape 5.
 */
public enum Permission {
    // Permissions communes / opérationnelles
    VOIR_DASHBOARD("Consulter le tableau de bord et les statistiques"),
    ENREGISTRER_NAISSANCE("Enregistrer une nouvelle déclaration de naissance"),
    LISTER_NAISSANCES("Consulter le registre général des naissances"),
    RECHERCHER_NAISSANCES("Effectuer des recherches multicritères"),
    CONSULTER_PARENTS("Consulter les informations parentales"),
    VOIR_STATISTIQUES("Analyser les indicateurs statistiques"),
    CONSULTER_REGISTRE("Consulter les registres communaux"),
    GERER_DOCUMENTS("Délivrer et imprimer les extraits d'actes"),
    CONSULTER_FICHES("Consulter les fiches individuelles complètes"),

    // Permissions avancées (Administrateur & Responsable)
    MODIFIER_DOSSIERS("Corriger ou modifier un dossier existant"),

    // Permissions strictes réservées à l'Administrateur
    GERER_UTILISATEURS("Créer, modifier ou désactiver des utilisateurs"),
    ADMINISTRER_UTILISATEURS("Gérer les rôles et permissions des comptes"),
    PARAMETRES_SYSTEME("Modifier les paramètres système sensibles"),
    SAUVEGARDE_BASE("Effectuer des sauvegardes / exports de la base de données"),
    SUPPRESSION_DEFINITIVE("Supprimer définitivement un enregistrement");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
