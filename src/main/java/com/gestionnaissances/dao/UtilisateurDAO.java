package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.Utilisateur;
import com.gestionnaissances.security.PasswordService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la persistance des utilisateurs dans la table SQLite 'utilisateurs'.
 * Conforme à la sécurisation des mots de passe (PBKDF2) et aux rôles de l'Étape 5.
 */
public class UtilisateurDAO {

    /**
     * Insère un nouvel utilisateur avec mot de passe haché et garantie d'unicité du login.
     */
    public boolean inserer(Utilisateur u) throws SQLException {
        if (u == null || u.getNomUtilisateur() == null || u.getNomUtilisateur().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire.");
        }

        String login = u.getNomUtilisateur().trim().toLowerCase();
        if (existeNomUtilisateur(login)) {
            throw new SQLException("Le nom d'utilisateur '" + login + "' existe déjà (contrainte d'unicité).");
        }

        // Si le mot de passe n'est pas encore haché en PBKDF2, on le hache immédiatement
        String hash = u.getMotDePasseHash();
        if (hash == null || !hash.startsWith("PBKDF2$")) {
            hash = PasswordService.hasher(hash != null ? hash : "Default@123");
            u.setMotDePasseHash(hash);
        }

        String sql = """
            INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe, mot_de_passe_hash, nom_complet, role, actif)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, login);
            pstmt.setString(2, hash); // Pour rétrocompatibilité avec l'ancienne colonne mot_de_passe
            pstmt.setString(3, hash); // Nouvelle colonne officielle mot_de_passe_hash
            pstmt.setString(4, u.getNomComplet() != null ? u.getNomComplet() : login);
            pstmt.setString(5, u.getRole() != null ? u.getRole() : "AGENT");
            pstmt.setInt(6, u.isActif() ? 1 : 0);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        u.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un nom d'utilisateur existe déjà en base.
     */
    public boolean existeNomUtilisateur(String nomUtilisateur) {
        if (nomUtilisateur == null) return false;
        String sql = "SELECT COUNT(*) AS total FROM utilisateurs WHERE LOWER(nom_utilisateur) = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomUtilisateur.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur existeNomUtilisateur : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Recherche un utilisateur par son identifiant unique.
     */
    public Utilisateur trouverParId(int id) {
        String sql = "SELECT * FROM utilisateurs WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireUtilisateur(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur trouverParId : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Recherche un utilisateur par son identifiant textuel (login insensible à la casse).
     */
    public Utilisateur trouverParNom(String nomUtilisateur) {
        if (nomUtilisateur == null) return null;
        String sql = "SELECT * FROM utilisateurs WHERE LOWER(nom_utilisateur) = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomUtilisateur.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireUtilisateur(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur trouverParNom : " + ex.getMessage());
        }
        return null;
    }

    /**
     * Met à jour la date et heure de dernière connexion d'un utilisateur.
     */
    public boolean mettreAJourDerniereConnexion(int id, String dateHeure) {
        String sql = "UPDATE utilisateurs SET derniere_connexion = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dateHeure);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur mettreAJourDerniereConnexion : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Modifie le mot de passe d'un utilisateur en stockant exclusivement son nouveau hash PBKDF2.
     */
    public boolean changerMotDePasse(int id, String nouveauHash) {
        if (nouveauHash == null || nouveauHash.trim().isEmpty()) return false;
        String sql = "UPDATE utilisateurs SET mot_de_passe_hash = ?, mot_de_passe = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nouveauHash);
            pstmt.setString(2, nouveauHash);
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur changerMotDePasse : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Active ou désactive un utilisateur.
     */
    public boolean setStatutActif(int id, boolean actif) {
        String sql = "UPDATE utilisateurs SET actif = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, actif ? 1 : 0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur setStatutActif : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Met à jour les informations administratives d'un compte (nom complet, rôle, statut).
     */
    public boolean modifier(Utilisateur u) {
        if (u == null || u.getId() <= 0) return false;
        String sql = "UPDATE utilisateurs SET nom_complet = ?, role = ?, actif = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, u.getNomComplet());
            pstmt.setString(2, u.getRole());
            pstmt.setInt(3, u.isActif() ? 1 : 0);
            pstmt.setInt(4, u.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur modifier : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Retourne la liste de tous les utilisateurs triés par identifiant.
     */
    public List<Utilisateur> listerTous() {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs ORDER BY id ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(extraireUtilisateur(rs));
            }
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur listerTous : " + ex.getMessage());
        }
        return liste;
    }

    /**
     * Compte le nombre total d'utilisateurs.
     */
    public int compter() {
        String sql = "SELECT COUNT(*) AS total FROM utilisateurs;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException ex) {
            System.err.println("[UtilisateurDAO] Erreur compter : " + ex.getMessage());
        }
        return 0;
    }

    private Utilisateur extraireUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNomUtilisateur(rs.getString("nom_utilisateur"));

        // Lecture prioritaire de mot_de_passe_hash, sinon repli sur mot_de_passe
        String hash = rs.getString("mot_de_passe_hash");
        if (hash == null || hash.isEmpty()) {
            hash = rs.getString("mot_de_passe");
        }
        u.setMotDePasseHash(hash);

        // Nom complet
        String nomComplet = rs.getString("nom_complet");
        u.setNomComplet(nomComplet != null ? nomComplet : rs.getString("nom_utilisateur"));

        // Rôle (normalisation vers les rôles officiels de l'Étape 5)
        String roleStr = rs.getString("role");
        u.setRole(roleStr);

        u.setActif(rs.getInt("actif") == 1);
        u.setDateCreation(rs.getString("date_creation"));
        u.setDerniereConnexion(rs.getString("derniere_connexion"));

        return u;
    }
}
