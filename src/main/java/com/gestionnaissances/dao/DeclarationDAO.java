package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.Declaration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des déclarations de naissance dans la table 'declarations'.
 */
public class DeclarationDAO {

    public boolean inserer(Declaration d) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return inserer(conn, d);
        }
    }

    public boolean inserer(Connection conn, Declaration d) throws SQLException {
        String sql = """
            INSERT INTO declarations (enfant_id, numero_declaration, annee_declaration, date_declaration, heure_declaration, nom_declarant, qualite_declarant, observations)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, d.getEnfantId());
            pstmt.setString(2, d.getNumeroDeclaration());
            pstmt.setInt(3, d.getAnneeDeclaration() > 0 ? d.getAnneeDeclaration() : java.time.LocalDate.now().getYear());
            pstmt.setString(4, d.getDateDeclaration() != null && !d.getDateDeclaration().isBlank() ? d.getDateDeclaration() : java.time.LocalDate.now().toString());
            pstmt.setString(5, d.getHeureDeclaration() != null ? d.getHeureDeclaration() : "");
            pstmt.setString(6, d.getNomDeclarant() != null && !d.getNomDeclarant().isBlank() ? d.getNomDeclarant() : "Déclarant légal");
            pstmt.setString(7, d.getQualiteDeclarant() != null && !d.getQualiteDeclarant().isBlank() ? d.getQualiteDeclarant() : "Père");
            pstmt.setString(8, d.getObservations() != null ? d.getObservations() : "");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        d.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean existeNumeroDeclaration(String numeroDeclaration) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return existeNumeroDeclaration(conn, numeroDeclaration);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean existeNumeroDeclaration(Connection conn, String numeroDeclaration) {
        if (numeroDeclaration == null || numeroDeclaration.isBlank()) return false;
        String sql = "SELECT COUNT(*) AS total FROM declarations WHERE numero_declaration = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroDeclaration.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public Declaration trouverParEnfantId(int enfantId) {
        String sql = "SELECT * FROM declarations WHERE enfant_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireDeclaration(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DeclarationDAO] Erreur trouverParEnfantId : " + ex.getMessage());
        }
        return null;
    }

    public Declaration trouverParNumero(String numeroDeclaration) {
        String sql = "SELECT * FROM declarations WHERE numero_declaration = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroDeclaration);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireDeclaration(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DeclarationDAO] Erreur trouverParNumero : " + ex.getMessage());
        }
        return null;
    }

    public List<Declaration> listerParAnnee(int annee) {
        List<Declaration> liste = new ArrayList<>();
        String sql = "SELECT * FROM declarations WHERE annee_declaration = ? ORDER BY id ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, annee);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(extraireDeclaration(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DeclarationDAO] Erreur listerParAnnee : " + ex.getMessage());
        }
        return liste;
    }

    public boolean mettreAJour(Declaration d) {
        String sql = """
            UPDATE declarations
            SET numero_declaration = ?, annee_declaration = ?, date_declaration = ?, heure_declaration = ?,
                nom_declarant = ?, qualite_declarant = ?, observations = ?
            WHERE enfant_id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getNumeroDeclaration());
            pstmt.setInt(2, d.getAnneeDeclaration());
            pstmt.setString(3, d.getDateDeclaration());
            pstmt.setString(4, d.getHeureDeclaration());
            pstmt.setString(5, d.getNomDeclarant());
            pstmt.setString(6, d.getQualiteDeclarant());
            pstmt.setString(7, d.getObservations());
            pstmt.setInt(8, d.getEnfantId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[DeclarationDAO] Erreur mettreAJour : " + ex.getMessage());
            return false;
        }
    }

    private Declaration extraireDeclaration(ResultSet rs) throws SQLException {
        Declaration d = new Declaration();
        d.setId(rs.getInt("id"));
        d.setEnfantId(rs.getInt("enfant_id"));
        d.setNumeroDeclaration(rs.getString("numero_declaration"));
        d.setAnneeDeclaration(rs.getInt("annee_declaration"));
        d.setDateDeclaration(rs.getString("date_declaration"));
        d.setHeureDeclaration(rs.getString("heure_declaration"));
        d.setNomDeclarant(rs.getString("nom_declarant"));
        d.setQualiteDeclarant(rs.getString("qualite_declarant"));
        d.setObservations(rs.getString("observations"));
        return d;
    }
}
