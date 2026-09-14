package com.gestionnaissances.security;

/**
 * Gestionnaire de session en mémoire de l'utilisateur actuellement connecté.
 * Stocke uniquement les informations essentielles (ID, login, nom complet, rôle).
 * Fournit des méthodes d'interrogation de permissions et de déconnexion.
 */
public class SessionUtilisateur {

    private static SessionUtilisateur instanceActuelle = null;

    private final int id;
    private final String nomUtilisateur;
    private final String nomComplet;
    private final Role role;
    private final long timestampConnexion;

    private SessionUtilisateur(int id, String nomUtilisateur, String nomComplet, Role role) {
        this.id = id;
        this.nomUtilisateur = nomUtilisateur;
        this.nomComplet = (nomComplet != null && !nomComplet.trim().isEmpty()) ? nomComplet : nomUtilisateur;
        this.role = role != null ? role : Role.AGENT;
        this.timestampConnexion = System.currentTimeMillis();
    }

    /**
     * Ouvre une nouvelle session utilisateur.
     */
    public static synchronized void ouvrirSession(int id, String nomUtilisateur, String nomComplet, Role role) {
        instanceActuelle = new SessionUtilisateur(id, nomUtilisateur, nomComplet, role);
    }

    /**
     * Récupère l'instance de la session active en cours.
     *
     * @return L'instance SessionUtilisateur ou null si aucun utilisateur n'est connecté.
     */
    public static synchronized SessionUtilisateur getInstance() {
        return instanceActuelle;
    }

    /**
     * Indique si un utilisateur est actuellement authentifié.
     */
    public static synchronized boolean estConnecte() {
        return instanceActuelle != null;
    }

    /**
     * Détruit la session courante (déconnexion).
     */
    public static synchronized void deconnexion() {
        instanceActuelle = null;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public Role getRole() {
        return role;
    }

    public long getTimestampConnexion() {
        return timestampConnexion;
    }

    /**
     * Vérifie si l'utilisateur en session détient une permission donnée.
     */
    public boolean aPermission(Permission permission) {
        return RolePermissions.aPermission(this.role, permission);
    }
}
