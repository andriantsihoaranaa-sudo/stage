package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.Naissance;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) principal pour les Actes de Naissance.
 * Assure la persistance locale SQLite et la synchronisation avec les nouvelles tables
 * relationnelles ('enfants', 'parents', 'declarations', 'enregistrements_commune')
 * tout en maintenant la rétro-compatibilité totale avec l'interface JavaFX existante.
 */
public class NaissanceDAO {

    public List<Naissance> listerTous() {
        List<Naissance> liste = new ArrayList<>();
        String sql = "SELECT * FROM declarations_naissance ORDER BY id DESC;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                liste.add(extraireNaissance(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur lors de la lecture des actes : " + e.getMessage());
        }
        return liste;
    }

    public List<Naissance> rechercher(String critere) {
        List<Naissance> liste = new ArrayList<>();
        String sql = """
            SELECT * FROM declarations_naissance 
            WHERE numero_acte LIKE ? 
               OR nom_enfant LIKE ? 
               OR prenoms_enfant LIKE ? 
               OR date_naissance LIKE ? 
               OR lieu_naissance LIKE ? 
            ORDER BY id DESC;
        """;

        String motif = "%" + (critere != null ? critere.trim() : "") + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, motif);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(extraireNaissance(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur lors de la recherche : " + e.getMessage());
        }
        return liste;
    }

    public boolean inserer(Naissance n) {
        String sqlLegacy = """
            INSERT INTO declarations_naissance (
                numero_acte, annee_registre, date_enregistrement,
                nom_enfant, prenoms_enfant, sexe, date_naissance, heure_naissance,
                lieu_naissance, nom_pere, prenom_pere, profession_pere, domicile_pere,
                nom_mere, prenom_mere, profession_mere, domicile_mere,
                nom_declarant, qualite_declarant, officier_etat_civil, observations
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            int legacyId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLegacy, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, n.getNumeroActe());
                pstmt.setInt(2, n.getAnneeRegistre());
                pstmt.setString(3, n.getDateEnregistrement());
                pstmt.setString(4, n.getNomEnfant());
                pstmt.setString(5, n.getPrenomsEnfant());
                pstmt.setString(6, n.getSexe());
                pstmt.setString(7, n.getDateNaissance());
                pstmt.setString(8, n.getHeureNaissance());
                pstmt.setString(9, n.getLieuNaissance());
                pstmt.setString(10, n.getNomPere());
                pstmt.setString(11, n.getPrenomPere());
                pstmt.setString(12, n.getProfessionPere());
                pstmt.setString(13, n.getDomicilePere());
                pstmt.setString(14, n.getNomMere());
                pstmt.setString(15, n.getPrenomMere());
                pstmt.setString(16, n.getProfessionMere());
                pstmt.setString(17, n.getDomicileMere());
                pstmt.setString(18, n.getNomDeclarant());
                pstmt.setString(19, n.getQualiteDeclarant());
                pstmt.setString(20, n.getOfficierEtatCivil());
                pstmt.setString(21, n.getObservations());

                int lignesAffectees = pstmt.executeUpdate();
                if (lignesAffectees > 0) {
                    try (ResultSet cles = pstmt.getGeneratedKeys()) {
                        if (cles.next()) {
                            legacyId = cles.getInt(1);
                            n.setId(legacyId);
                        }
                    }
                }
            }

            // Synchronisation avec les tables normalisées Étape 2 (enfants, parents, declarations, enregistrements_commune)
            if (legacyId > 0) {
                // 1. Inserer dans enfants
                String sqlEnfant = """
                    INSERT INTO enfants (identifiant_unique, numero_registre, nom, prenoms, sexe, date_naissance, heure_naissance, lieu_naissance, statut)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Enfant');
                """;
                int enfantId = -1;
                try (PreparedStatement pstmtEnfant = conn.prepareStatement(sqlEnfant, Statement.RETURN_GENERATED_KEYS)) {
                    String idu = String.format("IDU-%d-%04d", n.getAnneeRegistre(), legacyId);
                    pstmtEnfant.setString(1, idu);
                    pstmtEnfant.setString(2, n.getNumeroActe());
                    pstmtEnfant.setString(3, n.getNomEnfant());
                    pstmtEnfant.setString(4, n.getPrenomsEnfant());
                    pstmtEnfant.setString(5, n.getSexe());
                    pstmtEnfant.setString(6, n.getDateNaissance());
                    pstmtEnfant.setString(7, n.getHeureNaissance());
                    pstmtEnfant.setString(8, n.getLieuNaissance());
                    pstmtEnfant.executeUpdate();
                    try (ResultSet clesE = pstmtEnfant.getGeneratedKeys()) {
                        if (clesE.next()) {
                            enfantId = clesE.getInt(1);
                        }
                    }
                }

                if (enfantId > 0) {
                    // 2. Inserer dans parents
                    String sqlParents = """
                        INSERT INTO parents (enfant_id, nom_pere, prenom_pere, profession_pere, nom_mere, prenom_mere, profession_mere)
                        VALUES (?, ?, ?, ?, ?, ?, ?);
                    """;
                    try (PreparedStatement pstmtParents = conn.prepareStatement(sqlParents)) {
                        pstmtParents.setInt(1, enfantId);
                        pstmtParents.setString(2, n.getNomPere());
                        pstmtParents.setString(3, n.getPrenomPere());
                        pstmtParents.setString(4, n.getProfessionPere());
                        pstmtParents.setString(5, n.getNomMere());
                        pstmtParents.setString(6, n.getPrenomMere());
                        pstmtParents.setString(7, n.getProfessionMere());
                        pstmtParents.executeUpdate();
                    }

                    // 3. Inserer dans declarations
                    String sqlDec = """
                        INSERT INTO declarations (enfant_id, numero_declaration, annee_declaration, date_declaration, heure_declaration, nom_declarant, qualite_declarant, observations)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """;
                    try (PreparedStatement pstmtDec = conn.prepareStatement(sqlDec)) {
                        pstmtDec.setInt(1, enfantId);
                        pstmtDec.setString(2, "DEC-" + n.getNumeroActe());
                        pstmtDec.setInt(3, n.getAnneeRegistre());
                        pstmtDec.setString(4, n.getDateEnregistrement());
                        pstmtDec.setString(5, "08:30");
                        pstmtDec.setString(6, n.getNomDeclarant());
                        pstmtDec.setString(7, n.getQualiteDeclarant());
                        pstmtDec.setString(8, n.getObservations());
                        pstmtDec.executeUpdate();
                    }

                    // 4. Inserer dans enregistrements_commune
                    String sqlEnreg = """
                        INSERT INTO enregistrements_commune (enfant_id, nom_responsable, fonction_responsable, date_enregistrement, heure_enregistrement)
                        VALUES (?, ?, ?, ?, ?);
                    """;
                    try (PreparedStatement pstmtEnreg = conn.prepareStatement(sqlEnreg)) {
                        pstmtEnreg.setInt(1, enfantId);
                        pstmtEnreg.setString(2, n.getOfficierEtatCivil() != null ? n.getOfficierEtatCivil() : "Officier de l'État Civil");
                        pstmtEnreg.setString(3, "Officier d'État Civil");
                        pstmtEnreg.setString(4, n.getDateEnregistrement());
                        pstmtEnreg.setString(5, "09:00");
                        pstmtEnreg.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur lors de l'enregistrement de l'acte : " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        String selectNumActe = "SELECT numero_acte FROM declarations_naissance WHERE id = ?;";
        String sqlDeleteLegacy = "DELETE FROM declarations_naissance WHERE id = ?;";
        String sqlDeleteEnfant = "DELETE FROM enfants WHERE numero_registre = ?;";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            String numActe = null;

            try (PreparedStatement pstmtSel = conn.prepareStatement(selectNumActe)) {
                pstmtSel.setInt(1, id);
                try (ResultSet rs = pstmtSel.executeQuery()) {
                    if (rs.next()) {
                        numActe = rs.getString("numero_acte");
                    }
                }
            }

            if (numActe != null) {
                try (PreparedStatement pstmtEnfant = conn.prepareStatement(sqlDeleteEnfant)) {
                    pstmtEnfant.setString(1, numActe);
                    pstmtEnfant.executeUpdate();
                }
            }

            int lignes;
            try (PreparedStatement pstmtLegacy = conn.prepareStatement(sqlDeleteLegacy)) {
                pstmtLegacy.setInt(1, id);
                lignes = pstmtLegacy.executeUpdate();
            }

            conn.commit();
            return lignes > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur lors de la suppression de l'acte #" + id + " : " + e.getMessage());
            return false;
        }
    }

    public int compterActes() {
        String sql = "SELECT COUNT(*) AS total FROM declarations_naissance;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur lors du décompte : " + e.getMessage());
        }
        return 0;
    }

    private Naissance extraireNaissance(ResultSet rs) throws SQLException {
        Naissance n = new Naissance();
        n.setId(rs.getInt("id"));
        n.setNumeroActe(rs.getString("numero_acte"));
        n.setAnneeRegistre(rs.getInt("annee_registre"));
        n.setDateEnregistrement(rs.getString("date_enregistrement"));
        n.setNomEnfant(rs.getString("nom_enfant"));
        n.setPrenomsEnfant(rs.getString("prenoms_enfant"));
        n.setSexe(rs.getString("sexe"));
        n.setDateNaissance(rs.getString("date_naissance"));
        n.setHeureNaissance(rs.getString("heure_naissance"));
        n.setLieuNaissance(rs.getString("lieu_naissance"));
        n.setNomPere(rs.getString("nom_pere"));
        n.setPrenomPere(rs.getString("prenom_pere"));
        n.setProfessionPere(rs.getString("profession_pere"));
        n.setDomicilePere(rs.getString("domicile_pere"));
        n.setNomMere(rs.getString("nom_mere"));
        n.setPrenomMere(rs.getString("prenom_mere"));
        n.setProfessionMere(rs.getString("profession_mere"));
        n.setDomicileMere(rs.getString("domicile_mere"));
        n.setNomDeclarant(rs.getString("nom_declarant"));
        n.setQualiteDeclarant(rs.getString("qualite_declarant"));
        n.setOfficierEtatCivil(rs.getString("officier_etat_civil"));
        n.setObservations(rs.getString("observations"));
        return n;
    }
}
