package com.gestionnaissances;

import com.gestionnaissances.dao.*;
import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.exception.ValidationException;
import com.gestionnaissances.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests unitaires pour l'Étape 4 :
 * - Validation de l'enregistrement transactionnel complet d'un dossier dans SQLite (7 tables)
 * - Vérification de l'atomicité de la transaction (ROLLBACK en cas d'échec)
 * - Contrôles préalables d'unicité (numéro de registre, identifiant unique communal, déclaration)
 * - Génération automatique de l'identifiant unique communal
 */
public class TransactionEtape4Test {

    private final EnfantDAO enfantDAO = new EnfantDAO();
    private final ParentsDAO parentsDAO = new ParentsDAO();
    private final DeclarationDAO declarationDAO = new DeclarationDAO();
    private final EnregistrementCommuneDAO communeDAO = new EnregistrementCommuneDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();

    @BeforeAll
    public static void initialiser() {
        DatabaseManager.initializeDatabase();
    }

    @Test
    public void testEnregistrementDossierTransactionnelSucces() throws Exception {
        long timestamp = System.currentTimeMillis();
        String idUnique = "CIV-COMM-2025-" + (timestamp % 100000);
        String numReg = "REG-2025-" + (timestamp % 10000);
        String numDecl = "DECL-2025-" + (timestamp % 10000);
        String numDoc = "DOC-2025-" + (timestamp % 10000);

        Enfant enfant = new Enfant();
        enfant.setIdentifiantUnique(idUnique);
        enfant.setNumeroRegistre(numReg);
        enfant.setNom("TRAORÉ");
        enfant.setPrenoms("Awa Mariam");
        enfant.setSexe("F");
        enfant.setDateNaissance("2025-05-12");
        enfant.setHeureNaissance("14:30");
        enfant.setLieuNaissance("Maternité Municipale");
        enfant.setStatut("Enfant");

        ParentInfo parents = new ParentInfo();
        parents.setNomPere("TRAORÉ");
        parents.setPrenomPere("Ibrahim");
        parents.setDateNaissancePere("1990-04-15");
        parents.setAgePere(ParentInfo.calculerAge("1990-04-15"));
        parents.setProfessionPere("Commerçant");
        parents.setDomicilePere("Quartier Résidentiel");

        parents.setNomMere("KONE");
        parents.setPrenomMere("Salimata");
        parents.setDateNaissanceMere("1994-08-20");
        parents.setAgeMere(ParentInfo.calculerAge("1994-08-20"));
        parents.setProfessionMere("Enseignante");
        parents.setDomicileMere("Quartier Résidentiel");

        Declaration decl = new Declaration();
        decl.setNumeroDeclaration(numDecl);
        decl.setAnneeDeclaration(2025);
        decl.setDateDeclaration("2025-05-13");
        decl.setHeureDeclaration("10:00");
        decl.setNomDeclarant("TRAORÉ Ibrahim");
        decl.setQualiteDeclarant("Père");
        decl.setObservations("Déclaration dans le délai légal");

        EnregistrementCommune commune = new EnregistrementCommune();
        commune.setNomResponsable("M. Kouamé Adjoumani");
        commune.setFonctionResponsable("Adjoint au Maire délégué");
        commune.setDateEnregistrement("2025-05-13");
        commune.setHeureEnregistrement("10:15");
        commune.setFolio("F-12");
        commune.setTome("T-02");
        commune.setStatutValidation("VALIDE");

        DocumentDelivre doc = new DocumentDelivre();
        doc.setTypeDocument("Extrait d'acte");
        doc.setNumeroDocument(numDoc);
        doc.setDateDelivrance("2025-05-13");
        doc.setHeureDelivrance("10:30");
        doc.setNomPersonneReception("TRAORÉ Ibrahim");
        doc.setQualitePersonneReception("Père");
        doc.setConfirmationLivraison(true);

        DossierNaissance dossier = new DossierNaissance(enfant, parents, decl, commune, doc);

        boolean succes = enfantDAO.enregistrerDossierTransactionnel(dossier);
        assertTrue(succes, "La transaction d'enregistrement du dossier complet doit réussir.");

        int idEnfant = enfant.getId();
        assertTrue(idEnfant > 0, "L'enfant inséré doit obtenir un ID auto-généré positif.");
        assertEquals(idEnfant, parents.getEnfantId(), "L'ID de l'enfant doit être répercuté sur les parents.");
        assertEquals(idEnfant, decl.getEnfantId(), "L'ID de l'enfant doit être répercuté sur la déclaration.");
        assertEquals(idEnfant, commune.getEnfantId(), "L'ID de l'enfant doit être répercuté sur l'enregistrement communal.");
        assertEquals(idEnfant, doc.getEnfantId(), "L'ID de l'enfant doit être répercuté sur le document.");

        // Vérification de la persistance en base via chargerDossierComplet
        DossierNaissance charge = enfantDAO.chargerDossierComplet(idEnfant);
        assertNotNull(charge, "Le dossier complet doit pouvoir être rechargé depuis SQLite.");
        assertEquals("TRAORÉ", charge.getNom());
        assertEquals("Awa Mariam", charge.getPrenoms());
        assertEquals("F", charge.getSexe());
        assertEquals(numReg, charge.getNumeroRegistre());
        assertEquals(idUnique, charge.getIdentifiantUnique());

        assertNotNull(charge.getParents(), "Les informations des parents doivent être associées.");
        assertEquals("TRAORÉ", charge.getParents().getNomPere());
        assertEquals("KONE", charge.getParents().getNomMere());

        assertNotNull(charge.getDeclaration(), "La déclaration doit être associée.");
        assertEquals(numDecl, charge.getDeclaration().getNumeroDeclaration());

        assertNotNull(charge.getEnregistrementCommune(), "L'enregistrement communal doit être associé.");
        assertEquals("VALIDE", charge.getEnregistrementCommune().getStatutValidation());

        assertNotNull(charge.getDocumentDelivre(), "Le document délivré doit être associé.");
        assertEquals(numDoc, charge.getDocumentDelivre().getNumeroDocument());
        assertTrue(charge.getDocumentDelivre().isConfirmationLivraison());
    }

    @Test
    public void testAtomiciteTransactionEtRollbackEnCasDErreur() {
        long timestamp = System.currentTimeMillis();
        String idUnique1 = "CIV-COMM-2025-ROLL1-" + (timestamp % 10000);
        String numReg1 = "REG-ROLL1-" + (timestamp % 10000);
        String numDecl1 = "DECL-ROLL-UNIQUE-" + (timestamp % 10000);

        // 1. Enregistrer un premier dossier valide
        Enfant enfant1 = new Enfant(idUnique1, numReg1, "DIABATÉ", "Moussa", "M", "2025-01-10", "08:00", "Clinique", "Enfant");
        Declaration decl1 = new Declaration(0, numDecl1, 2025, "2025-01-10", "09:00", "DIABATÉ Sékou", "Père", "Déclaration initiale");
        DossierNaissance dossier1 = new DossierNaissance(enfant1, new ParentInfo(), decl1, new EnregistrementCommune(), new DocumentDelivre());

        assertDoesNotThrow(() -> enfantDAO.enregistrerDossierTransactionnel(dossier1));

        // 2. Tenter d'enregistrer un deuxième dossier avec des identifiants Enfant uniques
        // mais avec un numéro de déclaration déjà existant pour provoquer un échec au milieu de la transaction
        String idUnique2 = "CIV-COMM-2025-ROLL2-" + (timestamp % 10000);
        String numReg2 = "REG-ROLL2-" + (timestamp % 10000);

        Enfant enfant2 = new Enfant(idUnique2, numReg2, "KABORE", "Adama", "M", "2025-01-11", "08:30", "Clinique", "Enfant");
        // Même numéro de déclaration 'numDecl1' qui viole la contrainte UNIQUE
        Declaration decl2 = new Declaration(0, numDecl1, 2025, "2025-01-11", "09:30", "KABORE Paul", "Père", "Tentative doublon");
        DossierNaissance dossier2 = new DossierNaissance(enfant2, new ParentInfo(), decl2, new EnregistrementCommune(), new DocumentDelivre());

        // La transaction doit échouer (ValidationException ou SQLException)
        assertThrows(Exception.class, () -> enfantDAO.enregistrerDossierTransactionnel(dossier2),
                "L'insertion avec un numéro de déclaration en doublon doit échouer.");

        // CRUCIAL : Vérifier l'atomicité (ROLLBACK) ! L'enfant2 NE DOIT PAS exister dans la base !
        Enfant enfant2EnBase = enfantDAO.trouverParIdentifiantUnique(idUnique2);
        assertNull(enfant2EnBase, "En cas d'échec de la transaction, l'enfant2 ne doit pas avoir été inséré (ROLLBACK garanti).");

        Enfant enfant2ParReg = enfantDAO.trouverParNumeroRegistre(numReg2);
        assertNull(enfant2ParReg, "En cas d'échec de la transaction, le registre de l'enfant2 ne doit pas exister.");
    }

    @Test
    public void testControlesPrealablesDoublons() throws Exception {
        long timestamp = System.currentTimeMillis();
        String idUnique = "CIV-COMM-2025-DUP-" + (timestamp % 10000);
        String numReg = "REG-DUP-" + (timestamp % 10000);
        String numDecl = "DECL-DUP-" + (timestamp % 10000);

        assertFalse(enfantDAO.existeNumeroRegistre(numReg));
        assertFalse(enfantDAO.existeIdentifiantUnique(idUnique));
        assertFalse(declarationDAO.existeNumeroDeclaration(numDecl));

        Enfant enfant = new Enfant(idUnique, numReg, "BAMBA", "Affou", "F", "2025-02-01", "07:00", "Maternité", "Enfant");
        Declaration decl = new Declaration(0, numDecl, 2025, "2025-02-01", "08:00", "BAMBA Mamadou", "Père", "Déclaration");
        DossierNaissance dossier = new DossierNaissance(enfant, new ParentInfo(), decl, new EnregistrementCommune(), new DocumentDelivre());

        enfantDAO.enregistrerDossierTransactionnel(dossier);

        // Après insertion, les méthodes doivent renvoyer true
        assertTrue(enfantDAO.existeNumeroRegistre(numReg));
        assertTrue(enfantDAO.existeIdentifiantUnique(idUnique));
        assertTrue(declarationDAO.existeNumeroDeclaration(numDecl));
    }

    @Test
    public void testGenerationIdentifiantUniqueCommunal() {
        int annee = LocalDate.now().getYear();
        String idGenere = enfantDAO.genererProchainIdentifiantCommunal(annee);
        assertNotNull(idGenere);
        assertTrue(idGenere.startsWith("CIV-COMM-" + annee + "-"), "L'identifiant doit respecter le format CIV-COMM-AAAA-XXXXX");
        assertFalse(enfantDAO.existeIdentifiantUnique(idGenere), "L'identifiant généré doit être garanti unique");
    }

    /**
     * Test de persistance réelle dans SQLite :
     * 1. Crée un dossier fictif complet
     * 2. L'enregistre via la transaction atomique (commit réel)
     * 3. Ferme et recrée la connexion SQLite
     * 4. Recherche et recharge le dossier depuis une nouvelle instance de DAO
     * 5. Vérifie que l'intégralité des données des 5 tables sont présentes et identiques
     */
    @Test
    public void testPersistanceApresFermetureEtReouvertureConnexionSQLite() throws Exception {
        long timestamp = System.currentTimeMillis();
        String idUnique = "CIV-COMM-2025-PERSIST-" + (timestamp % 100000);
        String numReg = "REG-PERSIST-" + (timestamp % 10000);
        String numDecl = "DECL-PERSIST-" + (timestamp % 10000);
        String numDoc = "DOC-PERSIST-" + (timestamp % 10000);

        // 1. Création d'un dossier complet fictif avec données dans les 5 tables
        Enfant enfant = new Enfant();
        enfant.setIdentifiantUnique(idUnique);
        enfant.setNumeroRegistre(numReg);
        enfant.setNom("OUATTARA");
        enfant.setPrenoms("Seydou Karim");
        enfant.setSexe("M");
        enfant.setDateNaissance("2025-06-15");
        enfant.setHeureNaissance("11:45");
        enfant.setLieuNaissance("Hôpital Général");
        enfant.setStatut("Enfant");

        ParentInfo parents = new ParentInfo();
        parents.setNomPere("OUATTARA");
        parents.setPrenomPere("Bakary");
        parents.setDateNaissancePere("1988-03-22");
        parents.setAgePere(37);
        parents.setProfessionPere("Ingénieur agronome");
        parents.setDomicilePere("Quartier Administratif");

        parents.setNomMere("COULIBALY");
        parents.setPrenomMere("Fatoumata");
        parents.setDateNaissanceMere("1992-11-10");
        parents.setAgeMere(32);
        parents.setProfessionMere("Pharmacienne");
        parents.setDomicileMere("Quartier Administratif");

        Declaration decl = new Declaration();
        decl.setNumeroDeclaration(numDecl);
        decl.setAnneeDeclaration(2025);
        decl.setDateDeclaration("2025-06-16");
        decl.setHeureDeclaration("09:15");
        decl.setNomDeclarant("OUATTARA Bakary");
        decl.setQualiteDeclarant("Père");
        decl.setObservations("Déclaration effectuée sous les délais réglementaires");

        EnregistrementCommune commune = new EnregistrementCommune();
        commune.setNomResponsable("M. Jean-Marc YAO");
        commune.setFonctionResponsable("Officier d'État Civil Délégué");
        commune.setDateEnregistrement("2025-06-16");
        commune.setHeureEnregistrement("09:30");
        commune.setFolio("F-45");
        commune.setTome("T-03");
        commune.setStatutValidation("VALIDE");

        DocumentDelivre doc = new DocumentDelivre();
        doc.setTypeDocument("Extrait d'acte de naissance");
        doc.setNumeroDocument(numDoc);
        doc.setDateDelivrance("2025-06-16");
        doc.setHeureDelivrance("10:00");
        doc.setNomPersonneReception("OUATTARA Bakary");
        doc.setQualitePersonneReception("Père");
        doc.setConfirmationLivraison(true);

        DossierNaissance dossier = new DossierNaissance(enfant, parents, decl, commune, doc);

        // 2. Enregistrement transactionnel avec commit()
        boolean enregistre = enfantDAO.enregistrerDossierTransactionnel(dossier);
        assertTrue(enregistre, "Le dossier doit être enregistré avec succès et commité.");
        int enfantIdGenere = enfant.getId();
        assertTrue(enfantIdGenere > 0, "L'enfant doit avoir un ID auto-incrémenté valide.");

        // 3. Fermeture explicite et réouverture d'une nouvelle connexion SQLite
        try (java.sql.Connection connFermee = DatabaseManager.getConnection()) {
            assertFalse(connFermee.isClosed());
            connFermee.close();
            assertTrue(connFermee.isClosed(), "La connexion doit être fermée.");
        }

        // Réouverture d'une nouvelle connexion physique pour attester qu'aucune donnée n'est volatile
        try (java.sql.Connection nouvelleConn = DatabaseManager.getConnection()) {
            assertFalse(nouvelleConn.isClosed(), "Une nouvelle connexion physique à SQLite naissances.db doit être établie.");
        }

        // 4. Instanciation d'un nouveau DAO indépendant et relecture directe depuis la base disque
        EnfantDAO nouveauDao = new EnfantDAO();
        DossierNaissance dossierRelu = nouveauDao.chargerDossierComplet(enfantIdGenere);

        // 5. Vérification intégrale que toutes les données sont présentes dans les 5 tables
        assertNotNull(dossierRelu, "Le dossier doit persister sur le disque SQLite et être relu avec succès.");
        assertEquals("OUATTARA", dossierRelu.getNom());
        assertEquals("Seydou Karim", dossierRelu.getPrenoms());
        assertEquals("M", dossierRelu.getSexe());
        assertEquals("2025-06-15", dossierRelu.getDateNaissance());
        assertEquals("11:45", dossierRelu.getHeureNaissance());
        assertEquals("Hôpital Général", dossierRelu.getLieuNaissance());
        assertEquals(numReg, dossierRelu.getNumeroRegistre());
        assertEquals(idUnique, dossierRelu.getIdentifiantUnique());

        // Table parents
        assertNotNull(dossierRelu.getParents(), "La table parents doit persister.");
        assertEquals("OUATTARA", dossierRelu.getParents().getNomPere());
        assertEquals("Bakary", dossierRelu.getParents().getPrenomPere());
        assertEquals("Ingénieur agronome", dossierRelu.getParents().getProfessionPere());
        assertEquals("COULIBALY", dossierRelu.getParents().getNomMere());
        assertEquals("Fatoumata", dossierRelu.getParents().getPrenomMere());
        assertEquals("Pharmacienne", dossierRelu.getParents().getProfessionMere());

        // Table declarations
        assertNotNull(dossierRelu.getDeclaration(), "La table declarations doit persister.");
        assertEquals(numDecl, dossierRelu.getDeclaration().getNumeroDeclaration());
        assertEquals(2025, dossierRelu.getDeclaration().getAnneeDeclaration());
        assertEquals("OUATTARA Bakary", dossierRelu.getDeclaration().getNomDeclarant());

        // Table enregistrements_commune
        assertNotNull(dossierRelu.getEnregistrementCommune(), "La table enregistrements_commune doit persister.");
        assertEquals("M. Jean-Marc YAO", dossierRelu.getEnregistrementCommune().getNomResponsable());
        assertEquals("VALIDE", dossierRelu.getEnregistrementCommune().getStatutValidation());
        assertEquals("F-45", dossierRelu.getEnregistrementCommune().getFolio());
        assertEquals("T-03", dossierRelu.getEnregistrementCommune().getTome());

        // Table documents
        assertNotNull(dossierRelu.getDocumentDelivre(), "La table documents doit persister.");
        assertEquals(numDoc, dossierRelu.getDocumentDelivre().getNumeroDocument());
        assertEquals("Extrait d'acte de naissance", dossierRelu.getDocumentDelivre().getTypeDocument());
        assertTrue(dossierRelu.getDocumentDelivre().isConfirmationLivraison());

        // Recherche multicritères sur la base persistée
        java.util.List<DossierNaissance> resultats = nouveauDao.rechercherMultiCriteres(
                idUnique, null, "OUATTARA", "Seydou", "Bakary", "COULIBALY", "2025", "2025"
        );
        assertEquals(1, resultats.size(), "La recherche multicritères doit retrouver le dossier persisté.");
        assertEquals(enfantIdGenere, resultats.get(0).getId());
    }
}
