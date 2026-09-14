package com.gestionnaissances;

import com.gestionnaissances.dao.UtilisateurDAO;
import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.exception.ValidationException;
import com.gestionnaissances.model.Utilisateur;
import com.gestionnaissances.security.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests unitaires et d'intégration pour l'Étape 5 :
 * - Sécurité des mots de passe (PBKDF2-HMAC-SHA256, sel cryptographique)
 * - Matrice des rôles et contrôle d'accès fondé sur les rôles (RBAC)
 * - Gestion de la session utilisateur en mémoire (Singleton)
 * - Scénarios d'authentification (succès, mot de passe erroné, compte inactif, utilisateur inexistant)
 * - Procédure de changement de mot de passe sécurisée
 * - Persistance et intégrité dans la base SQLite locale (naissances.db)
 */
public class AuthentificationEtape5Test {

    private final AuthentificationService authService = new AuthentificationService();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    @BeforeAll
    public static void initialiserBase() {
        // Initialisation de la base SQLite et migration Étape 5
        DatabaseManager.initializeDatabase();
    }

    @BeforeEach
    public void nettoyerSession() {
        SessionUtilisateur.deconnexion();
    }

    // =========================================================================
    // 1. TESTS DU SERVICE DE HACHAGE (PBKDF2-HMAC-SHA256)
    // =========================================================================

    @Test
    public void testHachageEtVerificationMotDePasse() {
        String motDePasseClair = "SecretAdmin@2025";

        // Génération du hash sécurisé
        String hash = PasswordService.hasher(motDePasseClair);
        assertNotNull(hash, "Le hash généré ne doit pas être null");
        assertTrue(hash.startsWith("PBKDF2$65536$"), "Le hash doit respecter la structure PBKDF2 avec 65536 itérations");

        // Vérification avec le bon mot de passe
        assertTrue(PasswordService.verifier(motDePasseClair, hash), "Le mot de passe correct doit être validé");

        // Vérification avec un mauvais mot de passe
        assertFalse(PasswordService.verifier("MauvaisMotDePasse", hash), "Un mot de passe incorrect doit être rejeté");
        assertFalse(PasswordService.verifier("", hash), "Un mot de passe vide doit être rejeté");
        assertFalse(PasswordService.verifier(null, hash), "Un mot de passe null doit être rejeté");

        // Deux hachages du même mot de passe doivent être différents grâce au sel aléatoire
        String deuxiemeHash = PasswordService.hasher(motDePasseClair);
        assertNotEquals(hash, deuxiemeHash, "Deux hashs consécutifs doivent avoir des sels cryptographiques différents");
    }

    // =========================================================================
    // 2. TESTS DE LA MATRICE DES RÔLES & PERMISSIONS (RBAC)
    // =========================================================================

    @Test
    public void testPermissionsParRole() {
        // 1. ADMINISTRATEUR : Tous les privilèges
        assertTrue(RolePermissions.aPermission(Role.ADMINISTRATEUR, Permission.GERER_UTILISATEURS),
                "L'Administrateur doit pouvoir gérer les utilisateurs");
        assertTrue(RolePermissions.aPermission(Role.ADMINISTRATEUR, Permission.SUPPRESSION_DEFINITIVE),
                "L'Administrateur doit pouvoir effectuer des suppressions définitives");
        assertTrue(RolePermissions.aPermission(Role.ADMINISTRATEUR, Permission.MODIFIER_DOSSIERS),
                "L'Administrateur doit pouvoir modifier les dossiers");
        assertTrue(RolePermissions.aPermission(Role.ADMINISTRATEUR, Permission.ENREGISTRER_NAISSANCE),
                "L'Administrateur doit pouvoir enregistrer des naissances");

        // 2. RESPONSABLE_COMMUNAL : Supervision et validation, mais pas gestion utilisateurs ni suppression
        assertFalse(RolePermissions.aPermission(Role.RESPONSABLE_COMMUNAL, Permission.GERER_UTILISATEURS),
                "Le Responsable Communal ne doit pas pouvoir gérer les utilisateurs");
        assertFalse(RolePermissions.aPermission(Role.RESPONSABLE_COMMUNAL, Permission.SUPPRESSION_DEFINITIVE),
                "Le Responsable Communal ne doit pas pouvoir effectuer de suppression définitive");
        assertTrue(RolePermissions.aPermission(Role.RESPONSABLE_COMMUNAL, Permission.MODIFIER_DOSSIERS),
                "Le Responsable Communal doit pouvoir modifier/rectifier les dossiers");
        assertTrue(RolePermissions.aPermission(Role.RESPONSABLE_COMMUNAL, Permission.ENREGISTRER_NAISSANCE),
                "Le Responsable Communal doit pouvoir enregistrer des naissances");

        // 3. AGENT : Enregistrement, consultation et documents, mais pas modification de dossiers validés
        assertFalse(RolePermissions.aPermission(Role.AGENT, Permission.GERER_UTILISATEURS),
                "L'Agent ne doit pas accéder à l'administration des utilisateurs");
        assertFalse(RolePermissions.aPermission(Role.AGENT, Permission.MODIFIER_DOSSIERS),
                "L'Agent ne doit pas pouvoir modifier les dossiers d'actes enregistrés");
        assertTrue(RolePermissions.aPermission(Role.AGENT, Permission.ENREGISTRER_NAISSANCE),
                "L'Agent doit pouvoir enregistrer les déclarations de naissance");
        assertTrue(RolePermissions.aPermission(Role.AGENT, Permission.CONSULTER_REGISTRE),
                "L'Agent doit pouvoir consulter le registre");
        assertTrue(RolePermissions.aPermission(Role.AGENT, Permission.GENERER_DOCUMENTS),
                "L'Agent doit pouvoir générer les extraits officiels");
    }

    // =========================================================================
    // 3. TESTS DU SINGLETON DE SESSION UTILISATEUR
    // =========================================================================

    @Test
    public void testGestionSessionUtilisateur() {
        assertFalse(SessionUtilisateur.estConnecte(), "Aucune session ne doit être active initialement");

        // Ouverture d'une session
        SessionUtilisateur.ouvrirSession(42, "responsable.test", "Mme Le Maire Adjointe", Role.RESPONSABLE_COMMUNAL);
        assertTrue(SessionUtilisateur.estConnecte(), "La session doit être active");

        SessionUtilisateur session = SessionUtilisateur.getInstance();
        assertEquals(42, session.getId());
        assertEquals("responsable.test", session.getNomUtilisateur());
        assertEquals("Mme Le Maire Adjointe", session.getNomComplet());
        assertEquals(Role.RESPONSABLE_COMMUNAL, session.getRole());

        // Test de vérification de permission depuis la session
        assertTrue(session.aPermission(Permission.MODIFIER_DOSSIERS));
        assertFalse(session.aPermission(Permission.GERER_UTILISATEURS));

        // Déconnexion
        SessionUtilisateur.deconnexion();
        assertFalse(SessionUtilisateur.estConnecte(), "La session doit être terminée après déconnexion");
    }

    // =========================================================================
    // 4. TESTS DU PROCESSUS D'AUTHENTIFICATION AVEC SQLITE
    // =========================================================================

    @Test
    public void testAuthentificationSuccesAdmin() {
        // Connexion avec le compte administrateur initial
        AuthentificationResultat res = authService.authentifier("admin", "Admin@123");

        assertTrue(res.isSucces(), "L'authentification de l'admin doit réussir : " + res.getMessage());
        assertNotNull(res.getUtilisateur(), "L'utilisateur renvoyé ne doit pas être null");
        assertEquals("admin", res.getUtilisateur().getNomUtilisateur());
        assertEquals(Role.ADMINISTRATEUR, res.getRole());

        // Vérification de l'ouverture automatique de la session
        assertTrue(SessionUtilisateur.estConnecte());
        assertEquals("admin", SessionUtilisateur.getInstance().getNomUtilisateur());

        // Vérification de la date de dernière connexion mise à jour
        assertNotNull(res.getUtilisateur().getDerniereConnexion(), "La date de dernière connexion doit être renseignée");
    }

    @Test
    public void testAuthentificationEchecMotDePasseInvalide() {
        AuthentificationResultat res = authService.authentifier("admin", "FauxMotDePasse");

        assertFalse(res.isSucces(), "L'authentification doit échouer en cas de mot de passe incorrect");
        assertNull(res.getUtilisateur(), "Aucun utilisateur ne doit être instancié");
        assertFalse(SessionUtilisateur.estConnecte(), "Aucune session ne doit être ouverte");
        assertTrue(res.getMessage().contains("incorrect"), "Le message doit signaler un mot de passe incorrect");
    }

    @Test
    public void testAuthentificationEchecUtilisateurInexistant() {
        AuthentificationResultat res = authService.authentifier("utilisateur_inexistant_999", "Quelconque");

        assertFalse(res.isSucces(), "L'authentification doit échouer pour un compte inexistant");
        assertFalse(SessionUtilisateur.estConnecte(), "Aucune session ne doit être ouverte");
        assertTrue(res.getMessage().contains("introuvable") || res.getMessage().contains("incorrect"));
    }

    @Test
    public void testAuthentificationEchecCompteDesactive() throws SQLException {
        // Création d'un utilisateur désactivé de test
        long ts = System.currentTimeMillis();
        String login = "agent.bloque." + ts;
        String hash = PasswordService.hasher("Pass@123");
        Utilisateur u = new Utilisateur(login, hash, "Agent Bloqué", "AGENT", false);
        utilisateurDAO.inserer(u);

        AuthentificationResultat res = authService.authentifier(login, "Pass@123");
        assertFalse(res.isSucces(), "Un compte désactivé ne doit pas pouvoir se connecter");
        assertTrue(res.getMessage().toLowerCase().contains("désactivé"), "Le message doit mentionner la désactivation du compte");
        assertFalse(SessionUtilisateur.estConnecte());
    }

    // =========================================================================
    // 5. TESTS DE LA PROCÉDURE DE CHANGEMENT DE MOT DE PASSE
    // =========================================================================

    @Test
    public void testChangementMotDePasseSuccesEtControles() throws Exception {
        // Création d'un utilisateur dédié au test de changement de mot de passe
        long ts = System.currentTimeMillis();
        String login = "user.mdp." + ts;
        String mdpInitial = "Initial@123";
        String hashInitial = PasswordService.hasher(mdpInitial);

        Utilisateur u = new Utilisateur(login, hashInitial, "Utilisateur Test MDP", "AGENT", true);
        utilisateurDAO.inserer(u);

        Utilisateur cree = utilisateurDAO.rechercherParNomUtilisateur(login);
        assertNotNull(cree);
        int userId = cree.getId();

        // 1. Erreur : ancien mot de passe incorrect
        assertThrows(ValidationException.class, () -> {
            authService.changerMotDePasse(userId, "FauxActuel", "Nouveau@123", "Nouveau@123");
        }, "L'ancien mot de passe invalide doit lever une ValidationException");

        // 2. Erreur : discordance entre nouveau mot de passe et confirmation
        assertThrows(ValidationException.class, () -> {
            authService.changerMotDePasse(userId, mdpInitial, "Nouveau@123", "Discordance@456");
        }, "Une confirmation différente doit lever une ValidationException");

        // 3. Erreur : nouveau mot de passe trop court
        assertThrows(ValidationException.class, () -> {
            authService.changerMotDePasse(userId, mdpInitial, "abc", "abc");
        }, "Un mot de passe trop court doit lever une ValidationException");

        // 4. Succès : mise à jour avec nouveau mot de passe
        String nouveauMdp = "NouveauSecret@999";
        authService.changerMotDePasse(userId, mdpInitial, nouveauMdp, nouveauMdp);

        // Vérification que l'ancien mot de passe est désormais refusé
        AuthentificationResultat resAncien = authService.authentifier(login, mdpInitial);
        assertFalse(resAncien.isSucces(), "L'ancien mot de passe ne doit plus fonctionner");

        // Vérification que le nouveau mot de passe permet la connexion
        AuthentificationResultat resNouveau = authService.authentifier(login, nouveauMdp);
        assertTrue(resNouveau.isSucces(), "Le nouveau mot de passe doit fonctionner");

        // Vérification que dans SQLite, le mot de passe est bien stocké haché en PBKDF2 et non en clair
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT mot_de_passe_hash FROM utilisateurs WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String hashDb = rs.getString("mot_de_passe_hash");
                assertNotNull(hashDb);
                assertTrue(hashDb.startsWith("PBKDF2$"), "Le mot de passe doit être stocké en hash PBKDF2");
                assertNotEquals(nouveauMdp, hashDb, "Le mot de passe ne doit JAMAIS être stocké en clair");
            }
        }
    }

    // =========================================================================
    // 6. TEST DE L'UNICITÉ DU NOM D'UTILISATEUR
    // =========================================================================

    @Test
    public void testUniciteNomUtilisateur() {
        String loginUnique = "agent.unique." + System.currentTimeMillis();
        String hash = PasswordService.hasher("Passe@123");

        Utilisateur u1 = new Utilisateur(loginUnique, hash, "Premier Agent", "AGENT", true);
        assertDoesNotThrow(() -> utilisateurDAO.inserer(u1));

        // Tentative d'insérer un doublon avec le même login
        Utilisateur u2 = new Utilisateur(loginUnique, hash, "Deuxième Agent", "AGENT", true);
        assertThrows(SQLException.class, () -> utilisateurDAO.inserer(u2),
                "L'insertion d'un identifiant existant doit lever une SQLException de contrainte d'unicité");
    }
}
