package com.gestionnaissances.dao;

import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.exception.ValidationException;
import com.gestionnaissances.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO pour la gestion des données de la table 'enfants'.
 */
public class EnfantDAO {

    public int inserer(Enfant e) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return inserer(conn, e);
        }
    }

    public int inserer(Connection conn, Enfant e) throws SQLException {
        String sql = """
            INSERT INTO enfants (identifiant_unique, numero_registre, nom, prenoms, sexe, date_naissance, heure_naissance, lieu_naissance, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, e.getIdentifiantUnique());
            pstmt.setString(2, e.getNumeroRegistre());
            pstmt.setString(3, e.getNom());
            pstmt.setString(4, e.getPrenoms());
            pstmt.setString(5, e.getSexe());
            pstmt.setString(6, e.getDateNaissance());
            pstmt.setString(7, e.getHeureNaissance() != null ? e.getHeureNaissance() : "");
            pstmt.setString(8, e.getLieuNaissance());
            pstmt.setString(9, e.getStatut() != null && !e.getStatut().isBlank() ? e.getStatut() : "Enfant");

            pstmt.executeUpdate();
            try (ResultSet cles = pstmt.getGeneratedKeys()) {
                if (cles.next()) {
                    int id = cles.getInt(1);
                    e.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Vérifie si un numéro de registre existe déjà dans la base SQLite.
     */
    public boolean existeNumeroRegistre(String numeroRegistre) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return existeNumeroRegistre(conn, numeroRegistre);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean existeNumeroRegistre(Connection conn, String numeroRegistre) {
        if (numeroRegistre == null || numeroRegistre.isBlank()) return false;
        String sql = "SELECT COUNT(*) AS total FROM enfants WHERE numero_registre = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroRegistre.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Vérifie si un identifiant unique communal existe déjà dans la base SQLite.
     */
    public boolean existeIdentifiantUnique(String identifiantUnique) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return existeIdentifiantUnique(conn, identifiantUnique);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean existeIdentifiantUnique(Connection conn, String identifiantUnique) {
        if (identifiantUnique == null || identifiantUnique.isBlank()) return false;
        String sql = "SELECT COUNT(*) AS total FROM enfants WHERE identifiant_unique = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifiantUnique.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Génère automatiquement le prochain Identifiant unique communal au format strict :
     * CIV-COMM-YYYY-XXXXX (ex: CIV-COMM-2026-00001)
     * sans collision possible dans SQLite.
     */
    public String genererProchainIdentifiantCommunal(int annee) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return genererProchainIdentifiantCommunal(conn, annee);
        } catch (SQLException e) {
            return String.format("CIV-COMM-%d-00001", annee);
        }
    }

    public String genererProchainIdentifiantCommunal(Connection conn, int annee) {
        String prefix = String.format("CIV-COMM-%d-", annee);
        String sql = "SELECT identifiant_unique FROM enfants WHERE identifiant_unique LIKE ? ORDER BY id DESC LIMIT 100;";
        int maxNumero = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String idu = rs.getString("identifiant_unique");
                    if (idu != null && idu.startsWith(prefix)) {
                        String suffix = idu.substring(prefix.length());
                        try {
                            int num = Integer.parseInt(suffix);
                            if (num > maxNumero) {
                                maxNumero = num;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException ignored) {}

        // Boucle pour garantir l'unicité stricte
        int candidate = maxNumero + 1;
        while (true) {
            String candidateId = String.format("CIV-COMM-%d-%05d", annee, candidate);
            if (!existeIdentifiantUnique(conn, candidateId)) {
                return candidateId;
            }
            candidate++;
        }
    }

    /**
     * Enregistre un dossier de naissance complet de façon atomique et transactionnelle
     * dans les 5 tables correspondantes : enfants, parents, declarations, enregistrements_commune, documents.
     * En cas d'erreur ou d'échec sur l'une des tables, un ROLLBACK complet est effectué.
     */
    public boolean enregistrerDossierTransactionnel(DossierNaissance dossier) throws SQLException, ValidationException {
        if (dossier == null || dossier.getEnfant() == null) {
            throw new ValidationException("Le dossier ou les informations de l'enfant sont manquants.");
        }

        Enfant enfant = dossier.getEnfant();

        // 1. Validation des champs obligatoires de l'enfant
        if (enfant.getIdentifiantUnique() == null || enfant.getIdentifiantUnique().trim().isEmpty()) {
            throw new ValidationException("L'identifiant unique communal est obligatoire.");
        }
        if (enfant.getNumeroRegistre() == null || enfant.getNumeroRegistre().trim().isEmpty()) {
            throw new ValidationException("Le numéro de registre est obligatoire.");
        }
        if (enfant.getNom() == null || enfant.getNom().trim().isEmpty()) {
            throw new ValidationException("Veuillez renseigner le nom de l'enfant.");
        }
        if (enfant.getPrenoms() == null || enfant.getPrenoms().trim().isEmpty()) {
            throw new ValidationException("Veuillez renseigner les prénoms de l'enfant.");
        }
        if (enfant.getSexe() == null || enfant.getSexe().trim().isEmpty()) {
            throw new ValidationException("Veuillez sélectionner le sexe.");
        }
        if (enfant.getDateNaissance() == null || enfant.getDateNaissance().trim().isEmpty()) {
            throw new ValidationException("La date de naissance est obligatoire.");
        }
        if (enfant.getLieuNaissance() == null || enfant.getLieuNaissance().trim().isEmpty()) {
            throw new ValidationException("Le lieu de naissance est obligatoire.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // 2. Vérification préalable des doublons
            if (existeNumeroRegistre(conn, enfant.getNumeroRegistre())) {
                throw new ValidationException("Le numéro de registre existe déjà.");
            }
            if (existeIdentifiantUnique(conn, enfant.getIdentifiantUnique())) {
                throw new ValidationException("L'identifiant unique communal existe déjà.");
            }

            Declaration declaration = dossier.getDeclaration();
            DeclarationDAO decDAO = new DeclarationDAO();
            if (declaration != null && declaration.getNumeroDeclaration() != null && !declaration.getNumeroDeclaration().isBlank()) {
                if (decDAO.existeNumeroDeclaration(conn, declaration.getNumeroDeclaration())) {
                    throw new ValidationException("Le numéro de déclaration existe déjà.");
                }
            }

            // Début de la transaction SQLite
            conn.setAutoCommit(false);
            try {
                // Étape 3 & 4 : Insérer l'enfant et récupérer son id
                int enfantId = inserer(conn, enfant);
                if (enfantId <= 0) {
                    throw new SQLException("Impossible d'obtenir l'identifiant généré pour l'enfant.");
                }
                dossier.setId(enfantId);

                // Étape 5 : Insérer les informations des parents
                ParentInfo parents = dossier.getParents();
                if (parents == null) {
                    parents = new ParentInfo();
                }
                parents.setEnfantId(enfantId);
                ParentsDAO parentsDAO = new ParentsDAO();
                boolean parentsOk = parentsDAO.inserer(conn, parents);
                if (!parentsOk) {
                    throw new SQLException("Échec lors de l'enregistrement des informations parentales.");
                }

                // Étape 6 : Insérer la déclaration
                if (declaration == null) {
                    declaration = new Declaration();
                }
                declaration.setEnfantId(enfantId);
                boolean decOk = decDAO.inserer(conn, declaration);
                if (!decOk) {
                    throw new SQLException("Échec lors de l'enregistrement de la déclaration.");
                }

                // Étape 7 : Insérer l'enregistrement communal
                EnregistrementCommune commune = dossier.getCommune();
                if (commune == null) {
                    commune = new EnregistrementCommune();
                }
                commune.setEnfantId(enfantId);
                EnregistrementCommuneDAO commDAO = new EnregistrementCommuneDAO();
                boolean commOk = commDAO.inserer(conn, commune);
                if (!commOk) {
                    throw new SQLException("Échec lors de l'enregistrement communal officiel.");
                }

                // Étape 8 : Insérer le document délivré si renseigné
                DocumentDelivre document = dossier.getDocument();
                if (document != null && document.getNumeroDocument() != null && !document.getNumeroDocument().isBlank()) {
                    document.setEnfantId(enfantId);
                    DocumentDAO docDAO = new DocumentDAO();
                    boolean docOk = docDAO.inserer(conn, document);
                    if (!docOk) {
                        throw new SQLException("Échec lors de l'enregistrement du document délivré.");
                    }
                }

                // Étape 9 : COMMIT de la transaction atomique
                conn.commit();
                return true;

            } catch (Exception ex) {
                // ROLLBACK en cas d'erreur
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    System.err.println("[EnfantDAO] Erreur rollback : " + rbEx.getMessage());
                }
                if (ex instanceof ValidationException) {
                    throw (ValidationException) ex;
                }
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Erreur inattendue lors de la transaction : " + ex.getMessage(), ex);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {}
            }
        }
    }

    public Enfant trouverParId(int id) {
        String sql = "SELECT * FROM enfants WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireEnfant(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur trouverParId : " + ex.getMessage());
        }
        return null;
    }

    public Enfant trouverParIdentifiantUnique(String identifiantUnique) {
        String sql = "SELECT * FROM enfants WHERE identifiant_unique = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifiantUnique);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireEnfant(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur trouverParIdentifiantUnique : " + ex.getMessage());
        }
        return null;
    }

    public Enfant trouverParNumeroRegistre(String numeroRegistre) {
        String sql = "SELECT * FROM enfants WHERE numero_registre = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroRegistre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extraireEnfant(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur trouverParNumeroRegistre : " + ex.getMessage());
        }
        return null;
    }

    public List<Enfant> rechercher(String critere) {
        List<Enfant> liste = new ArrayList<>();
        String sql = """
            SELECT * FROM enfants 
            WHERE identifiant_unique LIKE ? 
               OR numero_registre LIKE ? 
               OR nom LIKE ? 
               OR prenoms LIKE ? 
               OR date_naissance LIKE ? 
               OR lieu_naissance LIKE ? 
            ORDER BY id DESC;
        """;

        String motif = "%" + (critere != null ? critere.trim() : "") + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                pstmt.setString(i, motif);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(extraireEnfant(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur recherche : " + ex.getMessage());
        }
        return liste;
    }

    public List<Enfant> listerTous() {
        List<Enfant> liste = new ArrayList<>();
        String sql = "SELECT * FROM enfants ORDER BY id DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(extraireEnfant(rs));
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur listerTous : " + ex.getMessage());
        }
        return liste;
    }

    public int compter() {
        String sql = "SELECT COUNT(*) AS total FROM enfants;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur compter : " + ex.getMessage());
        }
        return 0;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM enfants WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur suppression : " + ex.getMessage());
            return false;
        }
    }

    public boolean mettreAJourStatut(int id, String nouveauStatut) {
        String sql = "UPDATE enfants SET statut = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nouveauStatut);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur mise à jour statut : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Charge le dossier complet d'un enfant par son identifiant.
     */
    public DossierNaissance chargerDossierComplet(int enfantId) {
        Enfant enfant = trouverParId(enfantId);
        if (enfant == null) return null;

        ParentsDAO parentsDAO = new ParentsDAO();
        DeclarationDAO declarationDAO = new DeclarationDAO();
        EnregistrementCommuneDAO communeDAO = new EnregistrementCommuneDAO();
        DocumentDAO documentDAO = new DocumentDAO();

        ParentInfo parents = parentsDAO.trouverParEnfantId(enfantId);
        if (parents == null) parents = new ParentInfo();

        Declaration declaration = declarationDAO.trouverParEnfantId(enfantId);
        if (declaration == null) declaration = new Declaration();

        EnregistrementCommune commune = communeDAO.trouverParEnfantId(enfantId);
        if (commune == null) commune = new EnregistrementCommune();

        DocumentDelivre doc = documentDAO.trouverPremierParEnfantId(enfantId);
        if (doc == null) doc = new DocumentDelivre();

        return new DossierNaissance(enfant, parents, declaration, commune, doc);
    }

    /**
     * Liste tous les dossiers complets pour l'affichage dans les vues.
     */
    public List<DossierNaissance> listerDossiersComplets() {
        List<DossierNaissance> dossiers = new ArrayList<>();
        List<Enfant> enfants = listerTous();
        for (Enfant e : enfants) {
            dossiers.add(chargerDossierComplet(e.getId()));
        }
        return dossiers;
    }

    /**
     * Recherche multicritères sur l'ensemble des champs clés.
     */
    public List<DossierNaissance> rechercherMultiCriteres(String identifiantUnique, String numRegistre,
                                                          String nom, String prenom,
                                                          String nomPere, String nomMere,
                                                          String anneeNaissance, String anneeDeclaration) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT e.id FROM enfants e
            LEFT JOIN parents p ON p.enfant_id = e.id
            LEFT JOIN declarations d ON d.enfant_id = e.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (identifiantUnique != null && !identifiantUnique.isBlank()) {
            sql.append(" AND e.identifiant_unique LIKE ?");
            params.add("%" + identifiantUnique.trim() + "%");
        }
        if (numRegistre != null && !numRegistre.isBlank()) {
            sql.append(" AND e.numero_registre LIKE ?");
            params.add("%" + numRegistre.trim() + "%");
        }
        if (nom != null && !nom.isBlank()) {
            sql.append(" AND e.nom LIKE ?");
            params.add("%" + nom.trim() + "%");
        }
        if (prenom != null && !prenom.isBlank()) {
            sql.append(" AND e.prenoms LIKE ?");
            params.add("%" + prenom.trim() + "%");
        }
        if (nomPere != null && !nomPere.isBlank()) {
            sql.append(" AND (p.nom_pere LIKE ? OR p.prenom_pere LIKE ?)");
            params.add("%" + nomPere.trim() + "%");
            params.add("%" + nomPere.trim() + "%");
        }
        if (nomMere != null && !nomMere.isBlank()) {
            sql.append(" AND (p.nom_mere LIKE ? OR p.prenom_mere LIKE ?)");
            params.add("%" + nomMere.trim() + "%");
            params.add("%" + nomMere.trim() + "%");
        }
        if (anneeNaissance != null && !anneeNaissance.isBlank()) {
            sql.append(" AND e.date_naissance LIKE ?");
            params.add(anneeNaissance.trim() + "%");
        }
        if (anneeDeclaration != null && !anneeDeclaration.isBlank()) {
            sql.append(" AND (d.annee_declaration = ? OR e.date_creation LIKE ?)");
            try {
                params.add(Integer.parseInt(anneeDeclaration.trim()));
            } catch (NumberFormatException nfe) {
                params.add(0);
            }
            params.add(anneeDeclaration.trim() + "%");
        }

        sql.append(" ORDER BY e.id DESC;");

        List<DossierNaissance> resultats = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    resultats.add(chargerDossierComplet(id));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur rechercheMultiCriteres : " + ex.getMessage());
        }
        return resultats;
    }

    /**
     * Recherche globale textuelle (nom, prénom, numéro registre, identifiant).
     */
    public List<DossierNaissance> rechercherGlobal(String motCle) {
        if (motCle == null || motCle.isBlank()) {
            return listerDossiersComplets();
        }
        String sql = """
            SELECT DISTINCT e.id FROM enfants e
            LEFT JOIN parents p ON p.enfant_id = e.id
            LEFT JOIN declarations d ON d.enfant_id = e.id
            WHERE e.identifiant_unique LIKE ?
               OR e.numero_registre LIKE ?
               OR e.nom LIKE ?
               OR e.prenoms LIKE ?
               OR e.lieu_naissance LIKE ?
               OR p.nom_pere LIKE ?
               OR p.nom_mere LIKE ?
            ORDER BY e.id DESC;
        """;
        List<DossierNaissance> resultats = new ArrayList<>();
        String wildcard = "%" + motCle.trim() + "%";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 7; i++) {
                pstmt.setString(i, wildcard);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultats.add(chargerDossierComplet(rs.getInt("id")));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur rechercheGlobal : " + ex.getMessage());
        }
        return resultats;
    }

    /**
     * Nombre d'enfants par sexe.
     */
    public int compterParSexe(String sexe) {
        String sql = "SELECT COUNT(*) AS total FROM enfants WHERE UPPER(sexe) = UPPER(?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sexe);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur compterParSexe : " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Nombre de naissances déclarées pour l'année courante.
     */
    public int compterParAnnee(int annee) {
        String sql = "SELECT COUNT(*) AS total FROM enfants WHERE date_naissance LIKE ? OR date_creation LIKE ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, annee + "%");
            pstmt.setString(2, annee + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur compterParAnnee : " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Nombre de dossiers avec documents délivrés ou confirmés.
     */
    public int compterActesDelivres() {
        String sql = "SELECT COUNT(DISTINCT enfant_id) AS total FROM documents WHERE confirmation_livraison = 1 OR type_document IS NOT NULL;";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur compterActesDelivres : " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Répartition des naissances par année (pour les graphiques).
     */
    public Map<String, Integer> statistiquesParAnnee() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = """
            SELECT SUBSTR(date_naissance, 1, 4) AS annee, COUNT(*) AS nb
            FROM enfants
            WHERE date_naissance IS NOT NULL AND LENGTH(date_naissance) >= 4
            GROUP BY annee
            ORDER BY annee ASC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String annee = rs.getString("annee");
                if (annee != null && !annee.isBlank()) {
                    stats.put(annee, rs.getInt("nb"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur statistiquesParAnnee : " + ex.getMessage());
        }
        return stats;
    }

    /**
     * Répartition des naissances par mois pour une année donnée (pour les graphiques).
     */
    public Map<String, Integer> statistiquesMensuelles(int annee) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String[] moisNoms = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};
        for (String m : moisNoms) {
            stats.put(m, 0);
        }

        String sql = """
            SELECT SUBSTR(date_naissance, 6, 2) AS mois, COUNT(*) AS nb
            FROM enfants
            WHERE date_naissance LIKE ?
            GROUP BY mois;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, annee + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String moisStr = rs.getString("mois");
                    try {
                        int mIndex = Integer.parseInt(moisStr) - 1;
                        if (mIndex >= 0 && mIndex < 12) {
                            stats.put(moisNoms[mIndex], rs.getInt("nb"));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException ex) {
            System.err.println("[EnfantDAO] Erreur statistiquesMensuelles : " + ex.getMessage());
        }
        return stats;
    }

    private Enfant extraireEnfant(ResultSet rs) throws SQLException {
        Enfant e = new Enfant();
        e.setId(rs.getInt("id"));
        e.setIdentifiantUnique(rs.getString("identifiant_unique"));
        e.setNumeroRegistre(rs.getString("numero_registre"));
        e.setNom(rs.getString("nom"));
        e.setPrenoms(rs.getString("prenoms"));
        e.setSexe(rs.getString("sexe"));
        e.setDateNaissance(rs.getString("date_naissance"));
        e.setHeureNaissance(rs.getString("heure_naissance"));
        e.setLieuNaissance(rs.getString("lieu_naissance"));
        e.setStatut(rs.getString("statut"));
        e.setDateCreation(rs.getString("date_creation"));
        return e;
    }
}
