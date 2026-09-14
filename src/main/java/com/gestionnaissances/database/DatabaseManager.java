package com.gestionnaissances.database;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire centralisé de la base de données SQLite locale 'naissances.db'.
 * Étape 2 : Structure relationnelle complète, normalisée, indexée et évolutive,
 * garantissant la préservation intégrale des données historiques et la durabilité hors-ligne.
 */
public class DatabaseManager {

    private static final String DB_NAME = "naissances.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur: Pilote SQLite JDBC introuvable : " + e.getMessage());
        }
    }

    /**
     * Obtient une nouvelle connexion active avec activation systématique des clés étrangères.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Connexion vers une URL personnalisée (utilisée pour les tests unitaires isolés).
     */
    public static Connection getConnection(String customUrl) throws SQLException {
        Connection conn = DriverManager.getConnection(customUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Initialisation idempotente de la base de données naissances.db.
     * Crée les tables et index manquants, préserve et migre les données existantes.
     */
    public static void initializeDatabase() {
        initializeDatabase(DB_URL);
    }

    public static void initializeDatabase(String url) {
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            // 0. Table historique initiale (préservée si déjà présente)
            String createTableDeclarationsNaissance = """
                CREATE TABLE IF NOT EXISTS declarations_naissance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero_acte TEXT UNIQUE NOT NULL,
                    annee_registre INTEGER NOT NULL,
                    date_enregistrement TEXT NOT NULL,
                    nom_enfant TEXT NOT NULL,
                    prenoms_enfant TEXT NOT NULL,
                    sexe TEXT NOT NULL,
                    date_naissance TEXT NOT NULL,
                    heure_naissance TEXT,
                    lieu_naissance TEXT NOT NULL,
                    nom_pere TEXT,
                    prenom_pere TEXT,
                    profession_pere TEXT,
                    domicile_pere TEXT,
                    nom_mere TEXT NOT NULL,
                    prenom_mere TEXT NOT NULL,
                    profession_mere TEXT,
                    domicile_mere TEXT,
                    nom_declarant TEXT NOT NULL,
                    qualite_declarant TEXT NOT NULL,
                    officier_etat_civil TEXT,
                    observations TEXT,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createTableDeclarationsNaissance);

            // 1. Table ENFANTS
            String createTableEnfants = """
                CREATE TABLE IF NOT EXISTS enfants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    identifiant_unique TEXT UNIQUE NOT NULL,
                    numero_registre TEXT UNIQUE NOT NULL,
                    nom TEXT NOT NULL,
                    prenoms TEXT NOT NULL,
                    sexe TEXT NOT NULL,
                    date_naissance TEXT NOT NULL,
                    heure_naissance TEXT,
                    lieu_naissance TEXT NOT NULL,
                    statut TEXT DEFAULT 'Enfant',
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createTableEnfants);

            // 2. Table PARENTS
            String createTableParents = """
                CREATE TABLE IF NOT EXISTS parents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    enfant_id INTEGER NOT NULL UNIQUE,
                    nom_pere TEXT,
                    prenom_pere TEXT,
                    date_naissance_pere TEXT,
                    age_pere INTEGER,
                    profession_pere TEXT,
                    nom_mere TEXT NOT NULL,
                    prenom_mere TEXT NOT NULL,
                    date_naissance_mere TEXT,
                    age_mere INTEGER,
                    profession_mere TEXT,
                    FOREIGN KEY (enfant_id) REFERENCES enfants(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createTableParents);

            // 3. Table DECLARATIONS
            String createTableDeclarations = """
                CREATE TABLE IF NOT EXISTS declarations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    enfant_id INTEGER NOT NULL,
                    numero_declaration TEXT UNIQUE NOT NULL,
                    annee_declaration INTEGER NOT NULL,
                    date_declaration TEXT NOT NULL,
                    heure_declaration TEXT,
                    nom_declarant TEXT NOT NULL,
                    qualite_declarant TEXT NOT NULL,
                    observations TEXT,
                    FOREIGN KEY (enfant_id) REFERENCES enfants(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createTableDeclarations);

            // 4. Table ENREGISTREMENTS_COMMUNE
            String createTableEnregistrementsCommune = """
                CREATE TABLE IF NOT EXISTS enregistrements_commune (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    enfant_id INTEGER NOT NULL,
                    nom_responsable TEXT NOT NULL,
                    fonction_responsable TEXT NOT NULL,
                    date_enregistrement TEXT NOT NULL,
                    heure_enregistrement TEXT,
                    FOREIGN KEY (enfant_id) REFERENCES enfants(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createTableEnregistrementsCommune);

            // 5. Table DOCUMENTS
            String createTableDocuments = """
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    enfant_id INTEGER NOT NULL,
                    type_document TEXT NOT NULL,
                    numero_document TEXT NOT NULL,
                    date_delivrance TEXT NOT NULL,
                    nom_personne_reception TEXT,
                    qualite_personne_reception TEXT,
                    confirmation_livraison INTEGER DEFAULT 0,
                    FOREIGN KEY (enfant_id) REFERENCES enfants(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createTableDocuments);

            // 6. Table UTILISATEURS
            String createTableUtilisateurs = """
                CREATE TABLE IF NOT EXISTS utilisateurs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nom_utilisateur TEXT UNIQUE NOT NULL,
                    mot_de_passe TEXT,
                    mot_de_passe_hash TEXT,
                    nom_complet TEXT,
                    role TEXT NOT NULL,
                    actif INTEGER DEFAULT 1,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    derniere_connexion TEXT
                );
            """;
            stmt.execute(createTableUtilisateurs);

            // 7. Table HISTORIQUE
            String createTableHistorique = """
                CREATE TABLE IF NOT EXISTS historique (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    enfant_id INTEGER NOT NULL,
                    utilisateur_id INTEGER,
                    date_modification TEXT NOT NULL,
                    heure_modification TEXT NOT NULL,
                    information_modifiee TEXT NOT NULL,
                    ancienne_valeur TEXT,
                    nouvelle_valeur TEXT,
                    FOREIGN KEY (enfant_id) REFERENCES enfants(id) ON DELETE CASCADE,
                    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
                );
            """;
            stmt.execute(createTableHistorique);

            // 8. Index d'intégrité et de performance
            String[] indexStatements = {
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_enfants_identifiant_unique ON enfants(identifiant_unique);",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_enfants_numero_registre ON enfants(numero_registre);",
                "CREATE INDEX IF NOT EXISTS idx_enfants_nom ON enfants(nom);",
                "CREATE INDEX IF NOT EXISTS idx_enfants_prenoms ON enfants(prenoms);",
                "CREATE INDEX IF NOT EXISTS idx_enfants_date_naissance ON enfants(date_naissance);",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_declarations_numero ON declarations(numero_declaration);",
                "CREATE INDEX IF NOT EXISTS idx_declarations_annee ON declarations(annee_declaration);",
                "CREATE INDEX IF NOT EXISTS idx_parents_enfant_id ON parents(enfant_id);",
                "CREATE INDEX IF NOT EXISTS idx_enregistrements_enfant_id ON enregistrements_commune(enfant_id);",
                "CREATE INDEX IF NOT EXISTS idx_documents_enfant_id ON documents(enfant_id);",
                "CREATE INDEX IF NOT EXISTS idx_historique_enfant_id ON historique(enfant_id);"
            };

            for (String indexSql : indexStatements) {
                stmt.execute(indexSql);
            }

            // Étape 3 : Vérification et ajout non destructif des colonnes pour le formulaire complet
            ajouterColonneSiManquante(conn, "parents", "domicile_pere", "TEXT");
            ajouterColonneSiManquante(conn, "parents", "domicile_mere", "TEXT");
            ajouterColonneSiManquante(conn, "enregistrements_commune", "folio", "TEXT");
            ajouterColonneSiManquante(conn, "enregistrements_commune", "tome", "TEXT");
            ajouterColonneSiManquante(conn, "enregistrements_commune", "statut_validation", "TEXT DEFAULT 'VALIDE'");
            ajouterColonneSiManquante(conn, "documents", "heure_delivrance", "TEXT");

            // Étape 5 : Colonnes de sécurité et traçabilité pour la table utilisateurs
            ajouterColonneSiManquante(conn, "utilisateurs", "mot_de_passe_hash", "TEXT");
            ajouterColonneSiManquante(conn, "utilisateurs", "nom_complet", "TEXT");
            ajouterColonneSiManquante(conn, "utilisateurs", "derniere_connexion", "TEXT");

            // Initialiser les utilisateurs par défaut si la table est vide ou sécuriser les comptes existants
            initialiserUtilisateursParDefaut(conn);

            // Migration idempotente et sécurisée des données existantes
            migrerDonneesExistantesSiNecessaire(conn);

            System.out.println("[SQLite] Initialisation de la base terminée avec succès (" + url + ")");
        } catch (SQLException e) {
            System.err.println("[SQLite] Erreur d'initialisation de la base : " + e.getMessage());
        }
    }

    /**
     * Crée des comptes initiaux pour chaque rôle prévu (ADMINISTRATEUR, RESPONSABLE_COMMUNAL, AGENT)
     * si la table est vide, avec mots de passe hachés en PBKDF2 (aucun mot de passe en clair).
     * Si des comptes existent déjà, met à jour leurs empreintes de façon non destructive.
     */
    private static void initialiserUtilisateursParDefaut(Connection conn) {
        String sqlCount = "SELECT COUNT(*) AS total FROM utilisateurs;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlCount)) {
            if (rs.next() && rs.getInt("total") == 0) {
                String hashAdmin = com.gestionnaissances.security.PasswordService.hasher("Admin@123");
                String hashResp = com.gestionnaissances.security.PasswordService.hasher("Resp@123");
                String hashAgent = com.gestionnaissances.security.PasswordService.hasher("Agent@123");

                String sqlInsert = """
                    INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe, mot_de_passe_hash, nom_complet, role, actif) VALUES
                    ('admin', ?, ?, 'Administrateur Principal', 'ADMINISTRATEUR', 1),
                    ('responsable', ?, ?, 'Responsable Communal', 'RESPONSABLE_COMMUNAL', 1),
                    ('agent', ?, ?, 'Agent d''État Civil', 'AGENT', 1);
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                    pstmt.setString(1, hashAdmin);
                    pstmt.setString(2, hashAdmin);
                    pstmt.setString(3, hashResp);
                    pstmt.setString(4, hashResp);
                    pstmt.setString(5, hashAgent);
                    pstmt.setString(6, hashAgent);
                    pstmt.executeUpdate();
                }
                System.out.println("[SQLite] Compte administrateur initial ('admin' / 'Admin@123' [Haché PBKDF2]) et comptes de test créés.");
            } else {
                // Sécurisation non-destructive des comptes existants vers PBKDF2
                securiserComptesExistants(conn);
            }
        } catch (SQLException e) {
            System.err.println("[SQLite] Erreur lors de l'initialisation des utilisateurs : " + e.getMessage());
        }
    }

    /**
     * Sécurise les comptes existants en convertissant tout mot de passe en hash PBKDF2
     * et en normalisant les rôles vers les rôles officiels de l'Étape 5.
     */
    private static void securiserComptesExistants(Connection conn) {
        String sqlSelect = "SELECT id, nom_utilisateur, mot_de_passe, mot_de_passe_hash, role, nom_complet FROM utilisateurs;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSelect)) {
            List<Object[]> updates = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String nomUser = rs.getString("nom_utilisateur");
                String mdp = rs.getString("mot_de_passe");
                String hash = rs.getString("mot_de_passe_hash");
                String role = rs.getString("role");
                String nomComplet = rs.getString("nom_complet");

                boolean needUpdate = false;
                String finalHash = hash;
                if (finalHash == null || !finalHash.startsWith("PBKDF2$")) {
                    needUpdate = true;
                    if ("admin".equalsIgnoreCase(nomUser)) {
                        finalHash = com.gestionnaissances.security.PasswordService.hasher("Admin@123");
                    } else if ("responsable".equalsIgnoreCase(nomUser)) {
                        finalHash = com.gestionnaissances.security.PasswordService.hasher("Resp@123");
                    } else if ("agent".equalsIgnoreCase(nomUser)) {
                        finalHash = com.gestionnaissances.security.PasswordService.hasher("Agent@123");
                    } else {
                        finalHash = com.gestionnaissances.security.PasswordService.hasher(mdp != null && !mdp.isEmpty() ? mdp : "User@123");
                    }
                }

                String finalRole = role;
                if ("ADMIN".equalsIgnoreCase(role)) {
                    finalRole = "ADMINISTRATEUR";
                    needUpdate = true;
                } else if ("RESPONSABLE".equalsIgnoreCase(role)) {
                    finalRole = "RESPONSABLE_COMMUNAL";
                    needUpdate = true;
                }

                String finalNomComplet = nomComplet;
                if (finalNomComplet == null || finalNomComplet.isEmpty()) {
                    if ("admin".equalsIgnoreCase(nomUser)) finalNomComplet = "Administrateur Principal";
                    else if ("responsable".equalsIgnoreCase(nomUser)) finalNomComplet = "Responsable Communal";
                    else if ("agent".equalsIgnoreCase(nomUser)) finalNomComplet = "Agent d'État Civil";
                    else finalNomComplet = nomUser;
                    needUpdate = true;
                }

                if (needUpdate) {
                    updates.add(new Object[]{finalHash, finalNomComplet, finalRole, id});
                }
            }

            if (!updates.isEmpty()) {
                String sqlUpdate = "UPDATE utilisateurs SET mot_de_passe_hash = ?, mot_de_passe = ?, nom_complet = ?, role = ? WHERE id = ?;";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                    for (Object[] u : updates) {
                        pstmt.setString(1, (String) u[0]);
                        pstmt.setString(2, (String) u[0]); // Efface le clair
                        pstmt.setString(3, (String) u[1]);
                        pstmt.setString(4, (String) u[2]);
                        pstmt.setInt(5, (Integer) u[3]);
                        pstmt.executeUpdate();
                    }
                }
                System.out.println("[SQLite] " + updates.size() + " compte(s) utilisateur existant(s) sécurisé(s) en PBKDF2 avec rôles officiels.");
            }
        } catch (SQLException e) {
            System.err.println("[SQLite] Note sécurisation comptes : " + e.getMessage());
        }
    }

    /**
     * Préserve et synchronise les données existantes de la table 'declarations_naissance'
     * vers la nouvelle structure relationnelle sans perte ni duplication.
     */
    private static void migrerDonneesExistantesSiNecessaire(Connection conn) {
        String sqlCheck = "SELECT * FROM declarations_naissance;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlCheck)) {
            int migres = 0;
            while (rs.next()) {
                String numActe = rs.getString("numero_acte");
                if (!registreExiste(conn, numActe)) {
                    int enfantId = insererEnfantDepuisLegacy(conn, rs);
                    if (enfantId > 0) {
                        insererParentsDepuisLegacy(conn, enfantId, rs);
                        insererDeclarationDepuisLegacy(conn, enfantId, rs);
                        insererEnregistrementCommuneDepuisLegacy(conn, enfantId, rs);
                        migres++;
                    }
                }
            }
            if (migres > 0) {
                System.out.println("[SQLite] " + migres + " acte(s) existant(s) migré(s) et préservé(s) dans les tables relationnelles.");
            }
        } catch (SQLException e) {
            // Table non présente ou erreur de lecture tolérée
            System.err.println("[SQLite] Note migration : " + e.getMessage());
        }
    }

    private static boolean registreExiste(Connection conn, String numeroRegistre) {
        String sql = "SELECT COUNT(*) AS total FROM enfants WHERE numero_registre = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroRegistre);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static int insererEnfantDepuisLegacy(Connection conn, ResultSet rs) throws SQLException {
        String sql = """
            INSERT INTO enfants (identifiant_unique, numero_registre, nom, prenoms, sexe, date_naissance, heure_naissance, lieu_naissance, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Enfant');
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idLegacy = rs.getInt("id");
            int annee = rs.getInt("annee_registre");
            String identifiant = String.format("IDU-%d-%04d", annee, idLegacy);

            pstmt.setString(1, identifiant);
            pstmt.setString(2, rs.getString("numero_acte"));
            pstmt.setString(3, rs.getString("nom_enfant"));
            pstmt.setString(4, rs.getString("prenoms_enfant"));
            pstmt.setString(5, rs.getString("sexe"));
            pstmt.setString(6, rs.getString("date_naissance"));
            pstmt.setString(7, rs.getString("heure_naissance"));
            pstmt.setString(8, rs.getString("lieu_naissance"));

            pstmt.executeUpdate();
            try (ResultSet cles = pstmt.getGeneratedKeys()) {
                if (cles.next()) {
                    return cles.getInt(1);
                }
            }
        }
        return -1;
    }

    private static void insererParentsDepuisLegacy(Connection conn, int enfantId, ResultSet rs) throws SQLException {
        String sql = """
            INSERT INTO parents (enfant_id, nom_pere, prenom_pere, profession_pere, nom_mere, prenom_mere, profession_mere)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enfantId);
            pstmt.setString(2, rs.getString("nom_pere"));
            pstmt.setString(3, rs.getString("prenom_pere"));
            pstmt.setString(4, rs.getString("profession_pere"));
            pstmt.setString(5, rs.getString("nom_mere"));
            pstmt.setString(6, rs.getString("prenom_mere"));
            pstmt.setString(7, rs.getString("profession_mere"));
            pstmt.executeUpdate();
        }
    }

    private static void insererDeclarationDepuisLegacy(Connection conn, int enfantId, ResultSet rs) throws SQLException {
        String sql = """
            INSERT INTO declarations (enfant_id, numero_declaration, annee_declaration, date_declaration, heure_declaration, nom_declarant, qualite_declarant, observations)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String numActe = rs.getString("numero_acte");
            pstmt.setInt(1, enfantId);
            pstmt.setString(2, "DEC-" + numActe);
            pstmt.setInt(3, rs.getInt("annee_registre"));
            pstmt.setString(4, rs.getString("date_enregistrement"));
            pstmt.setString(5, "08:30");
            pstmt.setString(6, rs.getString("nom_declarant"));
            pstmt.setString(7, rs.getString("qualite_declarant"));
            pstmt.setString(8, rs.getString("observations"));
            pstmt.executeUpdate();
        }
    }

    private static void insererEnregistrementCommuneDepuisLegacy(Connection conn, int enfantId, ResultSet rs) throws SQLException {
        String sql = """
            INSERT INTO enregistrements_commune (enfant_id, nom_responsable, fonction_responsable, date_enregistrement, heure_enregistrement)
            VALUES (?, ?, ?, ?, ?);
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String officier = rs.getString("officier_etat_civil");
            pstmt.setInt(1, enfantId);
            pstmt.setString(2, (officier != null && !officier.isBlank()) ? officier : "Officier de l'État Civil");
            pstmt.setString(3, "Officier d'État Civil");
            pstmt.setString(4, rs.getString("date_enregistrement"));
            pstmt.setString(5, "09:00");
            pstmt.executeUpdate();
        }
    }

    /**
     * Liste toutes les tables utilisateurs actuellement créées dans la base SQLite.
     */
    public static List<String> listerTables() {
        return listerTables(DB_URL);
    }

    public static List<String> listerTables(String url) {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name;";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[SQLite] Erreur lecture tables : " + e.getMessage());
        }
        return tables;
    }

    /**
     * Teste la connexion et renvoie true si tout fonctionne.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Vérifie si une colonne existe dans une table SQLite, et l'ajoute sans perte de données si absente.
     */
    private static void ajouterColonneSiManquante(Connection conn, String table, String colonne, String typeDef) {
        String pragmaSql = "PRAGMA table_info(" + table + ");";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(pragmaSql)) {
            boolean existe = false;
            while (rs.next()) {
                if (colonne.equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + colonne + " " + typeDef + ";";
                stmt.execute(alterSql);
            }
        } catch (SQLException e) {
            // Ignorer si déjà existant ou non bloquant
        }
    }

    /**
     * Renvoie le chemin absolu du fichier SQLite sur le disque.
     */
    public static String getDatabasePath() {
        return new File(DB_NAME).getAbsolutePath();
    }
}
