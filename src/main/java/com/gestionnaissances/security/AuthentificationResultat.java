package com.gestionnaissances.security;

import com.gestionnaissances.model.Utilisateur;

/**
 * Résultat typé et sécurisé d'une tentative de connexion.
 */
public class AuthentificationResultat {

    public enum Statut {
        SUCCES("Connexion établie avec succès."),
        IDENTIFIANTS_MANQUANTS("Veuillez renseigner le nom d'utilisateur et le mot de passe."),
        UTILISATEUR_INEXISTANT("Nom d'utilisateur ou mot de passe incorrect."),
        MOT_DE_PASSE_INCORRECT("Nom d'utilisateur ou mot de passe incorrect."),
        COMPTE_DESACTIVE("Ce compte utilisateur est désactivé. Veuillez contacter l'administrateur."),
        ERREUR_TECHNIQUE("Une erreur technique est survenue lors de l'accès à la base.");

        private final String message;

        Statut(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Statut statut;
    private final Utilisateur utilisateur;
    private final String messageDetail;

    public AuthentificationResultat(Statut statut, Utilisateur utilisateur, String messageDetail) {
        this.statut = statut;
        this.utilisateur = utilisateur;
        this.messageDetail = messageDetail != null ? messageDetail : statut.getMessage();
    }

    public static AuthentificationResultat succes(Utilisateur utilisateur) {
        return new AuthentificationResultat(Statut.SUCCES, utilisateur, Statut.SUCCES.getMessage());
    }

    public static AuthentificationResultat echec(Statut statut) {
        return new AuthentificationResultat(statut, null, statut.getMessage());
    }

    public static AuthentificationResultat echec(Statut statut, String messageDetail) {
        return new AuthentificationResultat(statut, null, messageDetail);
    }

    public boolean isSucces() {
        return statut == Statut.SUCCES;
    }

    public Statut getStatut() {
        return statut;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public String getMessage() {
        return messageDetail;
    }
}
