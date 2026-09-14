package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.ParentInfo;

import java.sql.*;

/**
 * DAO pour la gestion des informations parentales dans la table 'parents'.
 */
public class ParentsDAO {

    public boolean inserer(ParentInfo p) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return inserer(conn, p);
        }
    }

    public boolean inserer(Connection conn, ParentInfo p) throws SQLException {
        String sql = """
            INSERT INTO parents (enfant_id, nom_pere, prenom_pere, date_naissance_pere, age_pere, profession_pere, domicile_pere,
                                 nom_mere, prenom_mere, date_naissance_mere, age_mere, profession_mere, domicile_mere)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, p.getEnfantId());
            pstmt.setString(2, p.getNomPere());
            pstmt.setString(3, p.getPrenomPere());
            pstmt.setString(4, p.getDateNaissancePere());
            pstmt.setInt(5, p.getAgePere());
            pstmt.setString(6, p.getProfessionPere());
            pstmt.setString(7, p.getDomicilePere() != null ? p.getDomicilePere() : "");
            pstmt.setString(8, p.getNomMere() != null && !p.getNomMere().isBlank() ? p.getNomMere() : "NON RENSEIGNÉE");
            pstmt.setString(9, p.getPrenomMere() != null ? p.getPrenomMere() : "");
            pstmt.setString(10, p.getDateNaissanceMere());
            pstmt.setInt(11, p.getAgeMere());
            pstmt.setString(12, p.getProfessionMere() != null ? p.getProfessionMere() : "");
            pstmt.setString(13, p.getDomicileMere() != null ? p.getDomicileMere() : "");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        p.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public ParentInfo trouverParEnfantId(int enfantId) {
        String sql = "SELECT * FROM parents WHERE enfant_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireParentInfo(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[ParentsDAO] Erreur trouverParEnfantId : " + ex.getMessage());
        }
        return null;
    }

    public boolean mettreAJour(ParentInfo p) {
        String sql = """
            UPDATE parents 
            SET nom_pere = ?, prenom_pere = ?, date_naissance_pere = ?, age_pere = ?, profession_pere = ?, domicile_pere = ?,
                nom_mere = ?, prenom_mere = ?, date_naissance_mere = ?, age_mere = ?, profession_mere = ?, domicile_mere = ?
            WHERE enfant_id = ?;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getNomPere());
            pstmt.setString(2, p.getPrenomPere());
            pstmt.setString(3, p.getDateNaissancePere());
            pstmt.setInt(4, p.getAgePere());
            pstmt.setString(5, p.getProfessionPere());
            pstmt.setString(6, p.getDomicilePere() != null ? p.getDomicilePere() : "");
            pstmt.setString(7, p.getNomMere());
            pstmt.setString(8, p.getPrenomMere());
            pstmt.setString(9, p.getDateNaissanceMere());
            pstmt.setInt(10, p.getAgeMere());
            pstmt.setString(11, p.getProfessionMere());
            pstmt.setString(12, p.getDomicileMere() != null ? p.getDomicileMere() : "");
            pstmt.setInt(13, p.getEnfantId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[ParentsDAO] Erreur mise à jour : " + ex.getMessage());
            return false;
        }
    }

    private ParentInfo extraireParentInfo(ResultSet rs) throws SQLException {
        ParentInfo p = new ParentInfo();
        p.setId(rs.getInt("id"));
        p.setEnfantId(rs.getInt("enfant_id"));
        p.setNomPere(rs.getString("nom_pere"));
        p.setPrenomPere(rs.getString("prenom_pere"));
        p.setDateNaissancePere(rs.getString("date_naissance_pere"));
        p.setAgePere(rs.getInt("age_pere"));
        p.setProfessionPere(rs.getString("profession_pere"));
        try {
            p.setDomicilePere(rs.getString("domicile_pere"));
        } catch (SQLException ignored) {}
        p.setNomMere(rs.getString("nom_mere"));
        p.setPrenomMere(rs.getString("prenom_mere"));
        p.setDateNaissanceMere(rs.getString("date_naissance_mere"));
        p.setAgeMere(rs.getInt("age_mere"));
        p.setProfessionMere(rs.getString("profession_mere"));
        try {
            p.setDomicileMere(rs.getString("domicile_mere"));
        } catch (SQLException ignored) {}
        return p;
    }
}
