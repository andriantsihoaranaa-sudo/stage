package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.EnregistrementCommune;

import java.sql.*;

/**
 * DAO pour la gestion des validations administratives dans la table 'enregistrements_commune'.
 */
public class EnregistrementCommuneDAO {

    public boolean inserer(EnregistrementCommune e) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return inserer(conn, e);
        }
    }

    public boolean inserer(Connection conn, EnregistrementCommune e) throws SQLException {
        String sql = """
            INSERT INTO enregistrements_commune (enfant_id, nom_responsable, fonction_responsable, date_enregistrement, heure_enregistrement, folio, tome, statut_validation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, e.getEnfantId());
            pstmt.setString(2, e.getNomResponsable() != null && !e.getNomResponsable().isBlank() ? e.getNomResponsable() : "Officier de l'État Civil");
            pstmt.setString(3, e.getFonctionResponsable() != null && !e.getFonctionResponsable().isBlank() ? e.getFonctionResponsable() : "Officier délégué");
            pstmt.setString(4, e.getDateEnregistrement() != null && !e.getDateEnregistrement().isBlank() ? e.getDateEnregistrement() : java.time.LocalDate.now().toString());
            pstmt.setString(5, e.getHeureEnregistrement() != null ? e.getHeureEnregistrement() : "");
            pstmt.setString(6, e.getFolio() != null ? e.getFolio() : "");
            pstmt.setString(7, e.getTome() != null ? e.getTome() : "");
            pstmt.setString(8, e.getStatutValidation() != null ? e.getStatutValidation() : "VALIDE");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        e.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public EnregistrementCommune trouverParEnfantId(int enfantId) {
        String sql = "SELECT * FROM enregistrements_commune WHERE enfant_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireEnregistrement(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnregistrementCommuneDAO] Erreur trouverParEnfantId : " + ex.getMessage());
        }
        return null;
    }

    public boolean mettreAJour(EnregistrementCommune e) {
        String sql = """
            UPDATE enregistrements_commune 
            SET nom_responsable = ?, fonction_responsable = ?, date_enregistrement = ?, heure_enregistrement = ?, folio = ?, tome = ?, statut_validation = ?
            WHERE enfant_id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, e.getNomResponsable());
            pstmt.setString(2, e.getFonctionResponsable());
            pstmt.setString(3, e.getDateEnregistrement());
            pstmt.setString(4, e.getHeureEnregistrement());
            pstmt.setString(5, e.getFolio() != null ? e.getFolio() : "");
            pstmt.setString(6, e.getTome() != null ? e.getTome() : "");
            pstmt.setString(7, e.getStatutValidation() != null ? e.getStatutValidation() : "VALIDE");
            pstmt.setInt(8, e.getEnfantId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[EnregistrementCommuneDAO] Erreur mise à jour : " + ex.getMessage());
            return false;
        }
    }

    private EnregistrementCommune extraireEnregistrement(ResultSet rs) throws SQLException {
        EnregistrementCommune e = new EnregistrementCommune();
        e.setId(rs.getInt("id"));
        e.setEnfantId(rs.getInt("enfant_id"));
        e.setNomResponsable(rs.getString("nom_responsable"));
        e.setFonctionResponsable(rs.getString("fonction_responsable"));
        e.setDateEnregistrement(rs.getString("date_enregistrement"));
        e.setHeureEnregistrement(rs.getString("heure_enregistrement"));
        try {
            e.setFolio(rs.getString("folio"));
            e.setTome(rs.getString("tome"));
            e.setStatutValidation(rs.getString("statut_validation"));
        } catch (SQLException ignored) {}
        return e;
    }
}
