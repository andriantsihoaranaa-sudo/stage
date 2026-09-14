package com.gestionnaissances.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour vérifier l'initialisation, la connectivité et la structure
 * des tables SQLite dans le cadre de l'Étape 2.
 */
public class DatabaseManagerTest {

    @BeforeAll
    public static void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @Test
    public void testConnexionEtInitialisation() {
        assertTrue(DatabaseManager.testConnection(), "La base de données SQLite doit être accessible et connectée.");

        try (Connection conn = DatabaseManager.getConnection()) {
            assertNotNull(conn, "L'objet Connection ne doit pas être nul.");
            assertFalse(conn.isClosed(), "La connexion doit être ouverte.");
        } catch (SQLException e) {
            fail("Exception SQL inattendue : " + e.getMessage());
        }
    }

    @Test
    public void testPresenceDesSeptTablesObligatoires() {
        List<String> tables = DatabaseManager.listerTables();

        assertTrue(tables.contains("enfants"), "La table 'enfants' doit exister.");
        assertTrue(tables.contains("parents"), "La table 'parents' doit exister.");
        assertTrue(tables.contains("declarations"), "La table 'declarations' doit exister.");
        assertTrue(tables.contains("enregistrements_commune"), "La table 'enregistrements_commune' doit exister.");
        assertTrue(tables.contains("documents"), "La table 'documents' doit exister.");
        assertTrue(tables.contains("utilisateurs"), "La table 'utilisateurs' doit exister.");
        assertTrue(tables.contains("historique"), "La table 'historique' doit exister.");
    }

    @Test
    public void testPragmaForeignKeysActive() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
            assertTrue(rs.next(), "Le pragma foreign_keys doit renvoyer un résultat.");
            assertEquals(1, rs.getInt(1), "Les clés étrangères doivent être activées (PRAGMA foreign_keys = 1).");
        }
    }

    @Test
    public void testInitialisationIdempotente() {
        // Exécuter l'initialisation plusieurs fois d'affilée ne doit ni lever d'erreur ni dupliquer
        assertDoesNotThrow(() -> {
            DatabaseManager.initializeDatabase();
            DatabaseManager.initializeDatabase();
        }, "L'initialisation doit être strictement idempotente.");
    }
}
