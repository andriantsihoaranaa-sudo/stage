package com.gestionnaissances;

import com.gestionnaissances.dao.*;
import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests unitaires validant l'architecture complète de l'Étape 2 :
 * - Insertion et recherche d'enfants
 * - Unicité de l'identifiant unique et du numéro de registre
 * - Relations clés étrangères (enfants -> parents, déclarations, documents)
 * - Calcul automatique de l'âge des parents depuis leur date de naissance
 * - Délivrance de documents et traçabilité historique
 */
public class ArchitectureEtape2Test {

    private final EnfantDAO enfantDAO = new EnfantDAO();
    private final ParentsDAO parentsDAO = new ParentsDAO();
    private final DeclarationDAO declarationDAO = new DeclarationDAO();
    private final EnregistrementCommuneDAO enregistrementCommuneDAO = new EnregistrementCommuneDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final HistoriqueDAO historiqueDAO = new HistoriqueDAO();

    @BeforeAll
    public static void initialiser() {
        DatabaseManager.initializeDatabase();
    }

    @Test
    public void testInsertionEtRechercheEnfant() throws SQLException {
        String numReg = "REG-TEST-" + System.currentTimeMillis();
        String idu = "IDU-TEST-" + System.currentTimeMillis();

        Enfant enfant = new Enfant(
                idu,
                numReg,
                "KONÉ",
                "Bakary Jean",
                "M",
                "2025-03-10",
                "06:45",
                "Hôpital Général",
                "Enfant"
        );

        int id = enfantDAO.inserer(enfant);
        assertTrue(id > 0, "L'insertion de l'enfant doit renvoyer un ID positif généré.");

        // Recherche par ID
        Enfant trouveParId = enfantDAO.trouverParId(id);
        assertNotNull(trouveParId);
        assertEquals("KONÉ", trouveParId.getNom());
        assertEquals("Bakary Jean", trouveParId.getPrenoms());

        // Recherche par identifiant unique
        Enfant trouveParIdu = enfantDAO.trouverParIdentifiantUnique(idu);
        assertNotNull(trouveParIdu);
        assertEquals(id, trouveParIdu.getId());

        // Recherche par numéro de registre
        Enfant trouveParReg = enfantDAO.trouverParNumeroRegistre(numReg);
        assertNotNull(trouveParReg);
        assertEquals("KONÉ", trouveParReg.getNom());

        // Recherche par motif texte
        List<Enfant> resultats = enfantDAO.rechercher("Bakary");
        assertFalse(resultats.isEmpty());
        assertTrue(resultats.stream().anyMatch(e -> e.getId() == id));
    }

    @Test
    public void testUniciteNumeroRegistreEtIdentifiantUnique() {
        String numReg = "REG-UNIQUE-TEST-" + System.currentTimeMillis();
        String idu = "IDU-UNIQUE-TEST-" + System.currentTimeMillis();

        Enfant enfant1 = new Enfant(idu, numReg, "OUATTARA", "Fatou", "F", "2025-01-01", "10:00", "Clinique", "Enfant");
        assertDoesNotThrow(() -> enfantDAO.inserer(enfant1));

        // Tentative d'insertion avec le même numéro de registre
        Enfant doublonReg = new Enfant("IDU-AUTRE-" + System.currentTimeMillis(), numReg, "COULIBALY", "Paul", "M", "2025-01-02", "11:00", "Clinique", "Enfant");
        assertThrows(SQLException.class, () -> enfantDAO.inserer(doublonReg),
                "L'insertion d'un numéro de registre dupliqué doit lever une exception d'unicité SQLite.");

        // Tentative d'insertion avec le même identifiant unique
        Enfant doublonIdu = new Enfant(idu, "REG-AUTRE-" + System.currentTimeMillis(), "COULIBALY", "Paul", "M", "2025-01-02", "11:00", "Clinique", "Enfant");
        assertThrows(SQLException.class, () -> enfantDAO.inserer(doublonIdu),
                "L'insertion d'un identifiant unique dupliqué doit lever une exception d'unicité SQLite.");
    }

    @Test
    public void testRelationEnfantParentsEtCalculAgeAutomatique() throws SQLException {
        String numReg = "REG-PARENT-" + System.currentTimeMillis();
        String idu = "IDU-PARENT-" + System.currentTimeMillis();

        Enfant enfant = new Enfant(idu, numReg, "DIABATÉ", "Sarah", "F", "2025-02-14", "15:20", "Maternité Nord", "Enfant");
        int enfantId = enfantDAO.inserer(enfant);

        // Date de naissance père : 1990-05-15 (doit calculer environ 35 ans)
        // Date de naissance mère : 1995-10-20 (doit calculer environ 30 ans)
        String datePere = "1990-05-15";
        String dateMere = "1995-10-20";
        int ageAttenduPere = ParentInfo.calculerAge(datePere);
        int ageAttenduMere = ParentInfo.calculerAge(dateMere);

        assertTrue(ageAttenduPere >= 34, "L'âge du père né en 1990 doit être calculé correctement.");
        assertTrue(ageAttenduMere >= 29, "L'âge de la mère née en 1995 doit être calculé correctement.");

        ParentInfo parents = new ParentInfo(
                enfantId,
                "DIABATÉ",
                "Mamadou",
                datePere,
                ageAttenduPere,
                "Architecte",
                "TOURE",
                "Aïcha",
                dateMere,
                ageAttenduMere,
                "Pharmacienne"
        );

        boolean insere = parentsDAO.inserer(parents);
        assertTrue(insere, "Les informations parentales doivent être enregistrées.");

        ParentInfo recup = parentsDAO.trouverParEnfantId(enfantId);
        assertNotNull(recup);
        assertEquals("DIABATÉ", recup.getNomPere());
        assertEquals(datePere, recup.getDateNaissancePere());
        assertEquals(ageAttenduPere, recup.getAgePere());
        assertEquals("TOURE", recup.getNomMere());
        assertEquals(dateMere, recup.getDateNaissanceMere());
        assertEquals(ageAttenduMere, recup.getAgeMere());
    }

    @Test
    public void testDeclarationEtEnregistrementCommune() throws SQLException {
        String numReg = "REG-COMMUNE-" + System.currentTimeMillis();
        String idu = "IDU-COMMUNE-" + System.currentTimeMillis();

        Enfant enfant = new Enfant(idu, numReg, "KABORE", "Adama", "M", "2025-04-01", "07:30", "Centre Hospitalier", "Enfant");
        int enfantId = enfantDAO.inserer(enfant);

        String numDec = "DEC-" + numReg;
        Declaration dec = new Declaration(
                enfantId,
                numDec,
                2025,
                "2025-04-02",
                "09:15",
                "KABORE Karim",
                "Père",
                "Certificat médical fourni"
        );

        boolean decInseree = declarationDAO.inserer(dec);
        assertTrue(decInseree, "La déclaration officielle doit être insérée avec succès.");

        // Unicité du numéro de déclaration
        Declaration decDoublon = new Declaration(
                enfantId,
                numDec,
                2025,
                "2025-04-02",
                "10:00",
                "Autre",
                "Père",
                "Observations"
        );
        assertThrows(SQLException.class, () -> declarationDAO.inserer(decDoublon),
                "Un numéro de déclaration identique doit violer la contrainte d'unicité SQLite.");

        // Enregistrement par la commune
        EnregistrementCommune enreg = new EnregistrementCommune(
                enfantId,
                "Mme Diallo Fatoumata",
                "Officier d'État Civil",
                "2025-04-02",
                "10:30"
        );
        boolean enregInsere = enregistrementCommuneDAO.inserer(enreg);
        assertTrue(enregInsere, "L'enregistrement communal doit être persisté.");

        EnregistrementCommune trouve = enregistrementCommuneDAO.trouverParEnfantId(enfantId);
        assertNotNull(trouve);
        assertEquals("Mme Diallo Fatoumata", trouve.getNomResponsable());
    }

    @Test
    public void testDocumentsDelivresEtHistorique() throws SQLException {
        String numReg = "REG-DOC-" + System.currentTimeMillis();
        String idu = "IDU-DOC-" + System.currentTimeMillis();

        Enfant enfant = new Enfant(idu, numReg, "YAO", "Koffi Emmanuel", "M", "2025-05-18", "12:00", "Clinique Sainte Marie", "Enfant");
        int enfantId = enfantDAO.inserer(enfant);

        // Document délivré
        DocumentDelivre doc = new DocumentDelivre(
                enfantId,
                "Extrait d'acte",
                "EXT-2025-0099",
                "2025-05-20",
                "YAO Amenan",
                "Mère",
                false
        );
        boolean docInsere = documentDAO.inserer(doc);
        assertTrue(docInsere, "Le document délivré doit être enregistré.");

        List<DocumentDelivre> docs = documentDAO.listerParEnfantId(enfantId);
        assertEquals(1, docs.size());
        assertFalse(docs.get(0).isConfirmationLivraison());

        // Confirmer la livraison
        boolean confirm = documentDAO.confirmerLivraison(docs.get(0).getId(), true);
        assertTrue(confirm);

        // Utilisateur & Historique de modification
        Utilisateur agent = utilisateurDAO.authentifier("agent", "agent123");
        assertNotNull(agent, "L'utilisateur agent par défaut doit être présent.");

        HistoriqueModification hist = new HistoriqueModification(
                enfantId,
                agent.getId(),
                LocalDate.now().toString(),
                "14:00",
                "lieu_naissance",
                "Clinique Sainte Marie",
                "Clinique Sainte Marie (Bâtiment B)"
        );

        boolean histOk = historiqueDAO.enregistrerModification(hist);
        assertTrue(histOk, "La modification doit être consignée dans la table historique.");

        List<HistoriqueModification> listeHist = historiqueDAO.listerParEnfantId(enfantId);
        assertEquals(1, listeHist.size());
        assertEquals("lieu_naissance", listeHist.get(0).getInformationModifiee());
        assertEquals("Clinique Sainte Marie", listeHist.get(0).getAncienneValeur());
    }
}
