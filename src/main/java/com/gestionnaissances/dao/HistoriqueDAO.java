package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.HistoriqueModification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la journalisation d'audit dans la table 'historique'.
 */
public class HistoriqueDAO {

    public boolean enregistrerModification(HistoriqueModification h) throws SQLException {
        String sql = """
            INSERT INTO historique (enfant_id, utilisateur_id, date_modification, heure_modification, information_modifiee, ancienne_valeur, nouvelle_valeur)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, h.getEnfantId());
            if (h.getUtilisateurId() > 0) {
                pstmt.setInt(2, h.getUtilisateurId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, h.getDateModification());
            pstmt.setString(4, h.getHeureModification());
            pstmt.setString(5, h.getInformationModifiee());
            pstmt.setString(6, h.getAncienneValeur());
            pstmt.setString(7, h.getNouvelleValeur());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        h.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<HistoriqueModification> listerParEnfantId(int enfantId) {
        List<HistoriqueModification> liste = new ArrayList<>();
        String sql = "SELECT * FROM historique WHERE enfant_id = ? ORDER BY id DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(extraireHistorique(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[HistoriqueDAO] Erreur listerParEnfantId : " + ex.getMessage());
        }
        return liste;
    }

    private HistoriqueModification extraireHistorique(ResultSet rs) throws SQLException {
        HistoriqueModification h = new HistoriqueModification();
        h.setId(rs.getInt("id"));
        h.setEnfantId(rs.getInt("enfant_id"));
        int uid = rs.getInt("utilisateur_id");
        if (!rs.wasNull()) {
            h.setUtilisateurId(uid);
        }
        h.setDateModification(rs.getString("date_modification"));
        h.setHeureModification(rs.getString("heure_modification"));
        h.setInformationModifiee(rs.getString("information_modifiee"));
        h.setAncienneValeur(rs.getString("ancienne_valeur"));
        h.setNouvelleValeur(rs.getString("nouvelle_valeur"));
        return h;
    }
}
