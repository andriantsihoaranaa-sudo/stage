package com.gestionnaissances.security;

import com.gestionnaissances.dao.UtilisateurDAO;
import com.gestionnaissances.exception.ValidationException;
import com.gestionnaissances.model.Utilisateur;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service orchestrant l'authentification, l'ouverture de session et le changement de mot de passe.
 *
 * Règles appliquées :
 * 1. Vérification de l'existence de l'utilisateur
 * 2. Vérification du statut actif du compte
 * 3. Vérification du mot de passe par empreinte sécurisée PBKDF2
 * 4. Détermination du rôle
 * 5. Enregistrement de l'horodatage de dernière connexion dans SQLite
 * 6. Création de la session en mémoire
 * 7. Aucun mot de passe en clair dans les logs
 */
public class AuthentificationService {

    private final UtilisateurDAO utilisateurDAO;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuthentificationService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }

    public AuthentificationService(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
    }

    /**
     * Tente d'authentifier un utilisateur et initialise la session en cas de succès.
     *
     * @param nomUtilisateur Nom d'utilisateur saisi
     * @param motDePasse     Mot de passe en clair saisi
     * @return Résultat structuré de la tentative
     */
    public AuthentificationResultat authentifier(String nomUtilisateur, String motDePasse) {
        if (nomUtilisateur == null || nomUtilisateur.trim().isEmpty() ||
            motDePasse == null || motDePasse.trim().isEmpty()) {
            return AuthentificationResultat.echec(AuthentificationResultat.Statut.IDENTIFIANTS_MANQUANTS);
        }

        String loginNormalise = nomUtilisateur.trim().toLowerCase();

        Utilisateur utilisateur = utilisateurDAO.trouverParNom(loginNormalise);
        if (utilisateur == null) {
            // Utilisateur introuvable
            return AuthentificationResultat.echec(AuthentificationResultat.Statut.UTILISATEUR_INEXISTANT);
        }

        // Vérification de l'état actif du compte
        if (!utilisateur.isActif()) {
            return AuthentificationResultat.echec(AuthentificationResultat.Statut.COMPTE_DESACTIVE);
        }

        // Vérification du mot de passe avec le hash
        String hashStocke = utilisateur.getMotDePasseHash();
        boolean valide = PasswordService.verifier(motDePasse, hashStocke);

        if (!valide) {
            return AuthentificationResultat.echec(AuthentificationResultat.Statut.MOT_DE_PASSE_INCORRECT);
        }

        // Connexion réussie : mise à jour de la dernière connexion
        String dateHeure = LocalDateTime.now().format(FORMATTER);
        utilisateurDAO.mettreAJourDerniereConnexion(utilisateur.getId(), dateHeure);
        utilisateur.setDerniereConnexion(dateHeure);

        // Création de la session en mémoire
        Role roleEnum = Role.depuisChaine(utilisateur.getRole());
        SessionUtilisateur.ouvrirSession(
                utilisateur.getId(),
                utilisateur.getNomUtilisateur(),
                utilisateur.getNomComplet(),
                roleEnum
        );

        return AuthentificationResultat.succes(utilisateur);
    }

    /**
     * Permet à un utilisateur de modifier son mot de passe de manière sécurisée.
     *
     * @param utilisateurId      ID de l'utilisateur
     * @param motDePasseActuel   Mot de passe actuel pour validation
     * @param nouveauMotDePasse  Nouveau mot de passe choisi
     * @param confirmation       Confirmation identique du nouveau mot de passe
     * @return true si la modification a réussi
     * @throws ValidationException Si la validation des règles échoue
     */
    public boolean changerMotDePasse(int utilisateurId, String motDePasseActuel,
                                     String nouveauMotDePasse, String confirmation) throws ValidationException {
        if (motDePasseActuel == null || motDePasseActuel.trim().isEmpty()) {
            throw new ValidationException("Le mot de passe actuel est obligatoire.");
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.trim().isEmpty()) {
            throw new ValidationException("Le nouveau mot de passe est obligatoire.");
        }
        if (nouveauMotDePasse.length() < 6) {
            throw new ValidationException("Le nouveau mot de passe doit comporter au moins 6 caractères.");
        }
        if (!nouveauMotDePasse.equals(confirmation)) {
            throw new ValidationException("Le nouveau mot de passe et sa confirmation ne correspondent pas.");
        }

        Utilisateur utilisateur = utilisateurDAO.trouverParId(utilisateurId);
        if (utilisateur == null) {
            throw new ValidationException("Utilisateur introuvable.");
        }

        // Vérification que le mot de passe actuel est correct
        if (!PasswordService.verifier(motDePasseActuel, utilisateur.getMotDePasseHash())) {
            throw new ValidationException("Le mot de passe actuel saisi est incorrect.");
        }

        // Génération du nouveau hash PBKDF2 sécurisé
        String nouveauHash = PasswordService.hasher(nouveauMotDePasse);

        // Mise à jour dans SQLite (uniquement le hash, jamais le mot de passe en clair)
        boolean modifie = utilisateurDAO.changerMotDePasse(utilisateurId, nouveauHash);
        if (!modifie) {
            throw new ValidationException("Échec de l'enregistrement du nouveau mot de passe en base.");
        }

        return true;
    }
}
