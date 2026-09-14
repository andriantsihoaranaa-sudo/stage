package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.DocumentDelivre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des documents officiels émis dans la table 'documents'.
 */
public class DocumentDAO {

    public boolean inserer(DocumentDelivre doc) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return inserer(conn, doc);
        }
    }

    public boolean inserer(Connection conn, DocumentDelivre doc) throws SQLException {
        String sql = """
            INSERT INTO documents (enfant_id, type_document, numero_document, date_delivrance, heure_delivrance, nom_personne_reception, qualite_personne_reception, confirmation_livraison)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, doc.getEnfantId());
            pstmt.setString(2, doc.getTypeDocument() != null && !doc.getTypeDocument().isBlank() ? doc.getTypeDocument() : "Extrait d'acte");
            pstmt.setString(3, doc.getNumeroDocument() != null && !doc.getNumeroDocument().isBlank() ? doc.getNumeroDocument() : "DOC-" + System.currentTimeMillis());
            pstmt.setString(4, doc.getDateDelivrance() != null && !doc.getDateDelivrance().isBlank() ? doc.getDateDelivrance() : java.time.LocalDate.now().toString());
            pstmt.setString(5, doc.getHeureDelivrance() != null ? doc.getHeureDelivrance() : "");
            pstmt.setString(6, doc.getNomPersonneReception() != null ? doc.getNomPersonneReception() : "");
            pstmt.setString(7, doc.getQualitePersonneReception() != null ? doc.getQualitePersonneReception() : "Déclarant");
            pstmt.setInt(8, doc.isConfirmationLivraison() ? 1 : 0);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet cles = pstmt.getGeneratedKeys()) {
                    if (cles.next()) {
                        doc.setId(cles.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<DocumentDelivre> listerParEnfantId(int enfantId) {
        List<DocumentDelivre> liste = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE enfant_id = ? ORDER BY id DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(extraireDocument(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DocumentDAO] Erreur listerParEnfantId : " + ex.getMessage());
        }
        return liste;
    }

    public DocumentDelivre trouverPremierParEnfantId(int enfantId) {
        String sql = "SELECT * FROM documents WHERE enfant_id = ? ORDER BY id ASC LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireDocument(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[DocumentDAO] Erreur trouverPremierParEnfantId : " + ex.getMessage());
        }
        return null;
    }

    public boolean mettreAJour(DocumentDelivre doc) {
        String sql = """
            UPDATE documents 
            SET type_document = ?, numero_document = ?, date_delivrance = ?, heure_delivrance = ?, nom_personne_reception = ?, qualite_personne_reception = ?, confirmation_livraison = ?
            WHERE enfant_id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doc.getTypeDocument());
            pstmt.setString(2, doc.getNumeroDocument());
            pstmt.setString(3, doc.getDateDelivrance());
            pstmt.setString(4, doc.getHeureDelivrance() != null ? doc.getHeureDelivrance() : "");
            pstmt.setString(5, doc.getNomPersonneReception());
            pstmt.setString(6, doc.getQualitePersonneReception());
            pstmt.setInt(7, doc.isConfirmationLivraison() ? 1 : 0);
            pstmt.setInt(8, doc.getEnfantId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[DocumentDAO] Erreur mise à jour : " + ex.getMessage());
            return false;
        }
    }

    public boolean confirmerLivraison(int documentId, boolean confirme) {
        String sql = "UPDATE documents SET confirmation_livraison = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, confirme ? 1 : 0);
            pstmt.setInt(2, documentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[DocumentDAO] Erreur confirmation livraison : " + ex.getMessage());
            return false;
        }
    }

    private DocumentDelivre extraireDocument(ResultSet rs) throws SQLException {
        DocumentDelivre doc = new DocumentDelivre();
        doc.setId(rs.getInt("id"));
        doc.setEnfantId(rs.getInt("enfant_id"));
        doc.setTypeDocument(rs.getString("type_document"));
        doc.setNumeroDocument(rs.getString("numero_document"));
        doc.setDateDelivrance(rs.getString("date_delivrance"));
        try {
            doc.setHeureDelivrance(rs.getString("heure_delivrance"));
        } catch (SQLException ignored) {}
        doc.setNomPersonneReception(rs.getString("nom_personne_reception"));
        doc.setQualitePersonneReception(rs.getString("qualite_personne_reception"));
        doc.setConfirmationLivraison(rs.getInt("confirmation_livraison") == 1);
        return doc;
    }
}
