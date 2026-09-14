package com.gestionnaissances.controller;

import com.gestionnaissances.dao.*;
import com.gestionnaissances.database.DatabaseManager;
import com.gestionnaissances.exception.ValidationException;
import com.gestionnaissances.model.*;
import com.gestionnaissances.security.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur JavaFX principal (Étape 3) :
 * Navigation latérale (Sidebar), gestion des 5 écrans (Tableau de bord, Formulaire,
 * Liste, Recherche multicritères, Fiche complète de l'enfant), et persistance SQLite hors-ligne.
 */
public class MainController implements Initializable {

    // DAOs d'accès aux 7 tables SQLite
    private final EnfantDAO enfantDAO = new EnfantDAO();
    private final ParentsDAO parentsDAO = new ParentsDAO();
    private final DeclarationDAO declarationDAO = new DeclarationDAO();
    private final EnregistrementCommuneDAO communeDAO = new EnregistrementCommuneDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();

    // Données observables
    private final ObservableList<DossierNaissance> tousLesDossiers = FXCollections.observableArrayList();
    private FilteredList<DossierNaissance> dossiersFiltres;
    private final ObservableList<DossierNaissance> resultatsRecherche = FXCollections.observableArrayList();
    private final ObservableList<DossierNaissance> derniersDossiers = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter affichageDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Dossier actuellement sélectionné ou affiché dans la fiche
    private DossierNaissance dossierActuel = null;

    // =========================================================================
    // ÉLÉMENTS DE STRUCTURE GLOBALE (TOP, SIDEBAR, STACK, BOTTOM)
    // =========================================================================
    @FXML private Label lblHeaderTitre;
    @FXML private StackPane mainContentStack;

    // Boutons du menu latéral
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavNouveau;
    @FXML private Button btnNavListe;
    @FXML private Button btnNavRecherche;
    @FXML private Button btnNavFiche;

    // Les 5 Vues
    @FXML private VBox viewDashboard;
    @FXML private VBox viewFormulaire;
    @FXML private VBox viewListe;
    @FXML private VBox viewRecherche;
    @FXML private VBox viewFiche;

    // Barre d'état
    @FXML private Label lblStatutDB;
    @FXML private Label lblTotalActes;
    @FXML private Label lblVueActuelle;
    @FXML private Label lblMessageInfo;

    // =========================================================================
    // SÉCURITÉ, AUTHENTIFICATION & GESTION DES UTILISATEURS (ÉTAPE 5)
    // =========================================================================
    @FXML private Label lblHeaderUtilisateur;
    @FXML private Label lblSidebarNomUtilisateur;
    @FXML private Label lblSidebarRoleBadge;
    @FXML private Button btnNavUtilisateurs;
    @FXML private Button btnSidebarChangerMdp;
    @FXML private Button btnSidebarDeconnexion;

    // 6. Vue : Gestion des utilisateurs (Réservé ADMINISTRATEUR)
    @FXML private VBox viewUtilisateurs;
    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, Number> colUserId;
    @FXML private TableColumn<Utilisateur, String> colUserNom;
    @FXML private TableColumn<Utilisateur, String> colUserNomComplet;
    @FXML private TableColumn<Utilisateur, String> colUserRole;
    @FXML private TableColumn<Utilisateur, String> colUserActif;
    @FXML private TableColumn<Utilisateur, String> colUserDateCreation;
    @FXML private TableColumn<Utilisateur, String> colUserDerniereConnexion;

    @FXML private TextField txtNewUserLogin;
    @FXML private TextField txtNewUserNomComplet;
    @FXML private ComboBox<String> cbNewUserRole;
    @FXML private PasswordField txtNewUserMdp;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final AuthentificationService authService = new AuthentificationService();
    private final ObservableList<Utilisateur> listeUtilisateurs = FXCollections.observableArrayList();

    // =========================================================================
    // 1. VUE : TABLEAU DE BORD (DASHBOARD)
    // =========================================================================
    @FXML private Label lblKpiTotal;
    @FXML private Label lblKpiGarcons;
    @FXML private Label lblKpiFilles;
    @FXML private Label lblKpiAnneeTitre;
    @FXML private Label lblKpiAnnee;

    @FXML private BarChart<String, Number> chartNaissancesParAnnee;
    @FXML private CategoryAxis chartAxisX;
    @FXML private NumberAxis chartAxisY;

    @FXML private TableView<DossierNaissance> tableDerniersDossiers;
    @FXML private TableColumn<DossierNaissance, String> colDashIdentifiant;
    @FXML private TableColumn<DossierNaissance, String> colDashNumeroRegistre;
    @FXML private TableColumn<DossierNaissance, String> colDashNomPrenoms;
    @FXML private TableColumn<DossierNaissance, String> colDashSexe;
    @FXML private TableColumn<DossierNaissance, String> colDashDateNaiss;
    @FXML private TableColumn<DossierNaissance, String> colDashLieu;
    @FXML private TableColumn<DossierNaissance, String> colDashMere;

    // =========================================================================
    // 2. VUE : FORMULAIRE D'ENREGISTREMENT
    // =========================================================================
    // Section 1 : Références
    @FXML private TextField txtFormIdentifiantUnique;
    @FXML private TextField txtFormNumeroRegistre;
    @FXML private TextField txtFormAnneeRegistre;
    @FXML private TextField txtFormFolio;
    @FXML private TextField txtFormTome;
    @FXML private DatePicker dpFormDateDeclaration;
    @FXML private TextField txtFormHeureDeclaration;

    // Section 2 : Identité enfant
    @FXML private TextField txtFormNomEnfant;
    @FXML private TextField txtFormPrenomsEnfant;
    @FXML private ComboBox<String> cbFormSexe;
    @FXML private ComboBox<String> cbFormStatutEnfant;
    @FXML private DatePicker dpFormDateNaissance;
    @FXML private TextField txtFormHeureNaissance;
    @FXML private TextField txtFormLieuNaissance;

    // Section 3 : Parents
    @FXML private TextField txtFormNomPere;
    @FXML private TextField txtFormPrenomPere;
    @FXML private DatePicker dpFormDateNaissPere;
    @FXML private TextField txtFormAgePere;
    @FXML private TextField txtFormProfessionPere;
    @FXML private TextField txtFormDomicilePere;

    @FXML private TextField txtFormNomMere;
    @FXML private TextField txtFormPrenomMere;
    @FXML private DatePicker dpFormDateNaissMere;
    @FXML private TextField txtFormAgeMere;
    @FXML private TextField txtFormProfessionMere;
    @FXML private TextField txtFormDomicileMere;

    // Section 4 : Déclarant
    @FXML private TextField txtFormNomDeclarant;
    @FXML private ComboBox<String> cbFormQualiteDeclarant;
    @FXML private TextField txtFormTemoin;
    @FXML private TextArea txtFormObservations;

    // Section 5 : Enregistrement communal & Document
    @FXML private TextField txtFormOfficier;
    @FXML private TextField txtFormFonction;
    @FXML private ComboBox<String> cbFormStatutValidation;
    @FXML private ComboBox<String> cbFormTypeDocument;
    @FXML private TextField txtFormNumeroDocument;
    @FXML private DatePicker dpFormDateDelivrance;
    @FXML private TextField txtFormHeureDelivrance;
    @FXML private TextField txtFormPersonneReception;
    @FXML private CheckBox chkFormConfirmationLivraison;

    // =========================================================================
    // 3. VUE : LISTE DES NAISSANCES
    // =========================================================================
    @FXML private TextField txtFiltreRapide;
    @FXML private ComboBox<String> cbFiltreSexe;
    @FXML private Label lblCompteurListe;

    @FXML private TableView<DossierNaissance> tableListeNaissances;
    @FXML private TableColumn<DossierNaissance, String> colListeIdentifiant;
    @FXML private TableColumn<DossierNaissance, String> colListeNumRegistre;
    @FXML private TableColumn<DossierNaissance, String> colListeNom;
    @FXML private TableColumn<DossierNaissance, String> colListePrenoms;
    @FXML private TableColumn<DossierNaissance, String> colListeSexe;
    @FXML private TableColumn<DossierNaissance, String> colListeDateNaiss;
    @FXML private TableColumn<DossierNaissance, String> colListeLieu;
    @FXML private TableColumn<DossierNaissance, String> colListePere;
    @FXML private TableColumn<DossierNaissance, String> colListeMere;
    @FXML private TableColumn<DossierNaissance, String> colListeStatut;

    // =========================================================================
    // 4. VUE : RECHERCHE MULTICRITÈRES
    // =========================================================================
    @FXML private TextField txtSearchIdentifiant;
    @FXML private TextField txtSearchNumRegistre;
    @FXML private TextField txtSearchNomEnfant;
    @FXML private TextField txtSearchPrenomsEnfant;
    @FXML private TextField txtSearchNomPere;
    @FXML private TextField txtSearchNomMere;
    @FXML private TextField txtSearchAnneeNaissance;
    @FXML private TextField txtSearchAnneeDeclaration;
    @FXML private Label lblResultatsRechercheCount;

    @FXML private TableView<DossierNaissance> tableResultatsRecherche;
    @FXML private TableColumn<DossierNaissance, String> colSearchIdentifiant;
    @FXML private TableColumn<DossierNaissance, String> colSearchNumRegistre;
    @FXML private TableColumn<DossierNaissance, String> colSearchNom;
    @FXML private TableColumn<DossierNaissance, String> colSearchPrenoms;
    @FXML private TableColumn<DossierNaissance, String> colSearchSexe;
    @FXML private TableColumn<DossierNaissance, String> colSearchDateNaiss;
    @FXML private TableColumn<DossierNaissance, String> colSearchLieu;
    @FXML private TableColumn<DossierNaissance, String> colSearchPere;
    @FXML private TableColumn<DossierNaissance, String> colSearchMere;

    // =========================================================================
    // 5. VUE : FICHE COMPLÈTE DE L'ENFANT
    // =========================================================================
    @FXML private Label lblFicheIdentifiantUnique;
    @FXML private Label lblFicheNumeroRegistre;
    @FXML private Label lblFicheNom;
    @FXML private Label lblFichePrenoms;
    @FXML private Label lblFicheSexe;
    @FXML private Label lblFicheDateHeureNaiss;
    @FXML private Label lblFicheLieuNaiss;
    @FXML private Label lblFicheStatutEnfant;

    @FXML private Label lblFicheNomPrenomPere;
    @FXML private Label lblFicheNaissAgePere;
    @FXML private Label lblFicheProfPere;
    @FXML private Label lblFicheDomPere;

    @FXML private Label lblFicheNomPrenomMere;
    @FXML private Label lblFicheNaissAgeMere;
    @FXML private Label lblFicheProfMere;
    @FXML private Label lblFicheDomMere;

    @FXML private Label lblFicheDeclarant;
    @FXML private Label lblFicheDateDeclaration;
    @FXML private Label lblFicheOfficier;
    @FXML private Label lblFicheTomeFolio;
    @FXML private Label lblFicheStatutValidation;
    @FXML private Label lblFicheObservations;

    @FXML private Label lblFicheTypeDoc;
    @FXML private Label lblFicheNumDoc;
    @FXML private Label lblFicheDateDoc;
    @FXML private Label lblFicheReceptionnaire;
    @FXML private Label lblFicheRemiseConfirmation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerOptionsComboBox();
        configurerColonnesTables();
        configurerCalculAgeAutomatique();
        configurerFiltresRechercheRapide();
        configurerDoubleClicTables();

        // Étape 5 : Initialisation et configuration des rôles et de la session
        configurerSessionEtRoles();

        // Réinitialisation du formulaire & chargement des données depuis SQLite
        reinitialiserFormulaireComplet();
        actualiserDonnees();
        verifierStatutBase();

        // Afficher la vue initiale : Tableau de bord
        afficherVueDashboard();
    }

    // =========================================================================
    // NAVIGATION ENTRE LES VUES & CONTRÔLE D'ACCÈS RBAC
    // =========================================================================

    private void desactiverTousBoutonsNav() {
        btnNavDashboard.getStyleClass().remove("sidebar-btn-active");
        btnNavNouveau.getStyleClass().remove("sidebar-btn-active");
        btnNavListe.getStyleClass().remove("sidebar-btn-active");
        btnNavRecherche.getStyleClass().remove("sidebar-btn-active");
        btnNavFiche.getStyleClass().remove("sidebar-btn-active");
        if (btnNavUtilisateurs != null) {
            btnNavUtilisateurs.getStyleClass().remove("sidebar-btn-active");
        }

        viewDashboard.setVisible(false);
        viewDashboard.setManaged(false);
        viewFormulaire.setVisible(false);
        viewFormulaire.setManaged(false);
        viewListe.setVisible(false);
        viewListe.setManaged(false);
        viewRecherche.setVisible(false);
        viewRecherche.setManaged(false);
        viewFiche.setVisible(false);
        viewFiche.setManaged(false);
        if (viewUtilisateurs != null) {
            viewUtilisateurs.setVisible(false);
            viewUtilisateurs.setManaged(false);
        }
    }

    @FXML
    public void afficherVueDashboard() {
        desactiverTousBoutonsNav();
        btnNavDashboard.getStyleClass().add("sidebar-btn-active");
        viewDashboard.setVisible(true);
        viewDashboard.setManaged(true);
        lblHeaderTitre.setText("Tableau de Bord — Registre d'État Civil");
        lblVueActuelle.setText("Vue : Tableau de bord");
        mettreAJourDashboard();
        afficherMessageInfo("Tableau de bord actualisé.", false);
    }

    @FXML
    public void afficherVueFormulaire() {
        desactiverTousBoutonsNav();
        btnNavNouveau.getStyleClass().add("sidebar-btn-active");
        viewFormulaire.setVisible(true);
        viewFormulaire.setManaged(true);
        lblHeaderTitre.setText("Enregistrer une Naissance — Saisie Complète");
        lblVueActuelle.setText("Vue : Formulaire d'enregistrement");
        afficherMessageInfo("Formulaire d'enregistrement prêt pour la saisie.", false);
    }

    @FXML
    public void afficherVueListe() {
        desactiverTousBoutonsNav();
        btnNavListe.getStyleClass().add("sidebar-btn-active");
        viewListe.setVisible(true);
        viewListe.setManaged(true);
        lblHeaderTitre.setText("Registre Général des Actes de Naissance");
        lblVueActuelle.setText("Vue : Liste des naissances");
        actualiserDonnees();
        afficherMessageInfo("Registre général : " + tousLesDossiers.size() + " dossier(s) chargé(s).", false);
    }

    @FXML
    public void afficherVueRecherche() {
        desactiverTousBoutonsNav();
        btnNavRecherche.getStyleClass().add("sidebar-btn-active");
        viewRecherche.setVisible(true);
        viewRecherche.setManaged(true);
        lblHeaderTitre.setText("Recherche Avancée Multicritères");
        lblVueActuelle.setText("Vue : Recherche multicritères");
        afficherMessageInfo("Module de recherche multicritères prêt.", false);
    }

    @FXML
    public void afficherVueFiche() {
        desactiverTousBoutonsNav();
        btnNavFiche.getStyleClass().add("sidebar-btn-active");
        viewFiche.setVisible(true);
        viewFiche.setManaged(true);
        lblHeaderTitre.setText("Fiche Complète Individuelle de l'Enfant");
        lblVueActuelle.setText("Vue : Fiche complète");
        if (dossierActuel == null && !tousLesDossiers.isEmpty()) {
            dossierActuel = tousLesDossiers.get(0);
        }
        remplirFicheOfficielle(dossierActuel);
    }

    @FXML
    public void afficherVueUtilisateurs() {
        if (!SessionUtilisateur.getInstance().aPermission(Permission.GERER_UTILISATEURS)) {
            afficherAlerte("Accès Refusé", "Permissions insuffisantes",
                    "Seul un Administrateur est habilité à accéder à la gestion des utilisateurs du système.",
                    Alert.AlertType.WARNING);
            return;
        }

        desactiverTousBoutonsNav();
        if (btnNavUtilisateurs != null) {
            btnNavUtilisateurs.getStyleClass().add("sidebar-btn-active");
        }
        if (viewUtilisateurs != null) {
            viewUtilisateurs.setVisible(true);
            viewUtilisateurs.setManaged(true);
        }
        lblHeaderTitre.setText("Administration des Comptes et Utilisateurs");
        lblVueActuelle.setText("Vue : Gestion des utilisateurs");
        chargerListeUtilisateurs();
        afficherMessageInfo("Module d'administration des utilisateurs chargé.", false);
    }

    // =========================================================================
    // INITIALISATION & CONFIGURATION DES COMPOSANTS
    // =========================================================================

    private void configurerOptionsComboBox() {
        // Formulaire
        cbFormSexe.setItems(FXCollections.observableArrayList("Masculin (M)", "Féminin (F)"));
        cbFormStatutEnfant.setItems(FXCollections.observableArrayList("Enfant", "Légitime", "Reconnu", "Adopté"));
        cbFormQualiteDeclarant.setItems(FXCollections.observableArrayList(
                "Père", "Mère", "Sage-femme", "Médecin", "Tuteur", "Grand-parent", "Autre"
        ));
        cbFormStatutValidation.setItems(FXCollections.observableArrayList("VALIDE", "EN ATTENTE"));
        cbFormTypeDocument.setItems(FXCollections.observableArrayList(
                "Extrait d'acte", "Acte de naissance", "Copie", "Autre"
        ));

        // Filtre Liste
        cbFiltreSexe.setItems(FXCollections.observableArrayList("Tous sexes", "Masculin (M)", "Féminin (F)"));
        cbFiltreSexe.setValue("Tous sexes");

        // Rôles utilisateurs
        if (cbNewUserRole != null) {
            cbNewUserRole.setItems(FXCollections.observableArrayList(
                    "ADMINISTRATEUR",
                    "RESPONSABLE_COMMUNAL",
                    "AGENT"
            ));
            cbNewUserRole.setValue("AGENT");
        }
    }

    private void configurerColonnesTables() {
        // 1. Table Tableau de bord (Derniers dossiers)
        colDashIdentifiant.setCellValueFactory(c -> c.getValue().identifiantUniqueProperty());
        colDashNumeroRegistre.setCellValueFactory(c -> c.getValue().numeroRegistreProperty());
        colDashNomPrenoms.setCellValueFactory(c -> {
            DossierNaissance d = c.getValue();
            return new javafx.beans.property.SimpleStringProperty(d.getNom() + " " + d.getPrenoms());
        });
        colDashSexe.setCellValueFactory(c -> c.getValue().sexeProperty());
        colDashDateNaiss.setCellValueFactory(c -> c.getValue().dateNaissanceProperty());
        colDashLieu.setCellValueFactory(c -> c.getValue().lieuNaissanceProperty());
        colDashMere.setCellValueFactory(c -> c.getValue().nomMereProperty());
        tableDerniersDossiers.setItems(derniersDossiers);

        // 2. Table Liste principale
        colListeIdentifiant.setCellValueFactory(c -> c.getValue().identifiantUniqueProperty());
        colListeNumRegistre.setCellValueFactory(c -> c.getValue().numeroRegistreProperty());
        colListeNom.setCellValueFactory(c -> c.getValue().nomProperty());
        colListePrenoms.setCellValueFactory(c -> c.getValue().prenomsProperty());
        colListeSexe.setCellValueFactory(c -> c.getValue().sexeProperty());
        colListeDateNaiss.setCellValueFactory(c -> c.getValue().dateNaissanceProperty());
        colListeLieu.setCellValueFactory(c -> c.getValue().lieuNaissanceProperty());
        colListePere.setCellValueFactory(c -> c.getValue().nomPereProperty());
        colListeMere.setCellValueFactory(c -> c.getValue().nomMereProperty());
        colListeStatut.setCellValueFactory(c -> c.getValue().statutProperty());

        dossiersFiltres = new FilteredList<>(tousLesDossiers, p -> true);
        tableListeNaissances.setItems(dossiersFiltres);

        // 3. Table Recherche multicritères
        colSearchIdentifiant.setCellValueFactory(c -> c.getValue().identifiantUniqueProperty());
        colSearchNumRegistre.setCellValueFactory(c -> c.getValue().numeroRegistreProperty());
        colSearchNom.setCellValueFactory(c -> c.getValue().nomProperty());
        colSearchPrenoms.setCellValueFactory(c -> c.getValue().prenomsProperty());
        colSearchSexe.setCellValueFactory(c -> c.getValue().sexeProperty());
        colSearchDateNaiss.setCellValueFactory(c -> c.getValue().dateNaissanceProperty());
        colSearchLieu.setCellValueFactory(c -> c.getValue().lieuNaissanceProperty());
        colSearchPere.setCellValueFactory(c -> c.getValue().nomPereProperty());
        colSearchMere.setCellValueFactory(c -> c.getValue().nomMereProperty());
        tableResultatsRecherche.setItems(resultatsRecherche);

        // 4. Table Utilisateurs (Étape 5)
        if (tableUtilisateurs != null) {
            if (colUserId != null) colUserId.setCellValueFactory(c -> c.getValue().idProperty());
            if (colUserNom != null) colUserNom.setCellValueFactory(c -> c.getValue().nomUtilisateurProperty());
            if (colUserNomComplet != null) colUserNomComplet.setCellValueFactory(c -> c.getValue().nomCompletProperty());
            if (colUserRole != null) colUserRole.setCellValueFactory(c -> c.getValue().roleProperty());
            if (colUserActif != null) colUserActif.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(c.getValue().isActif() ? "Actif" : "Désactivé"));
            if (colUserDateCreation != null) colUserDateCreation.setCellValueFactory(c -> c.getValue().dateCreationProperty());
            if (colUserDerniereConnexion != null) colUserDerniereConnexion.setCellValueFactory(c -> c.getValue().derniereConnexionProperty());
            tableUtilisateurs.setItems(listeUtilisateurs);
        }
    }

    private void configurerCalculAgeAutomatique() {
        // Calcul automatique de l'âge du père
        dpFormDateNaissPere.valueProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null) {
                int age = Period.between(nouv, LocalDate.now()).getYears();
                txtFormAgePere.setText(age > 0 ? String.valueOf(age) : "0");
            } else {
                txtFormAgePere.clear();
            }
        });

        // Calcul automatique de l'âge de la mère
        dpFormDateNaissMere.valueProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null) {
                int age = Period.between(nouv, LocalDate.now()).getYears();
                txtFormAgeMere.setText(age > 0 ? String.valueOf(age) : "0");
            } else {
                txtFormAgeMere.clear();
            }
        });
    }

    private void configurerFiltresRechercheRapide() {
        txtFiltreRapide.textProperty().addListener((obs, anc, nouv) -> appliquerFiltreListe());
        cbFiltreSexe.valueProperty().addListener((obs, anc, nouv) -> appliquerFiltreListe());
    }

    private void appliquerFiltreListe() {
        String motCle = txtFiltreRapide.getText();
        String filtreSexe = cbFiltreSexe.getValue();

        dossiersFiltres.setPredicate(d -> {
            if (d == null) return false;

            // Filtre par sexe
            if (filtreSexe != null && !filtreSexe.equals("Tous sexes")) {
                String codeSexe = filtreSexe.startsWith("F") ? "F" : "M";
                if (!codeSexe.equalsIgnoreCase(d.getSexe())) {
                    return false;
                }
            }

            // Filtre textuel global
            if (motCle == null || motCle.isBlank()) {
                return true;
            }
            String q = motCle.toLowerCase();
            return (d.getIdentifiantUnique() != null && d.getIdentifiantUnique().toLowerCase().contains(q))
                    || (d.getNumeroRegistre() != null && d.getNumeroRegistre().toLowerCase().contains(q))
                    || (d.getNom() != null && d.getNom().toLowerCase().contains(q))
                    || (d.getPrenoms() != null && d.getPrenoms().toLowerCase().contains(q))
                    || (d.getLieuNaissance() != null && d.getLieuNaissance().toLowerCase().contains(q))
                    || (d.getNomPere() != null && d.getNomPere().toLowerCase().contains(q))
                    || (d.getNomMere() != null && d.getNomMere().toLowerCase().contains(q));
        });

        lblCompteurListe.setText(dossiersFiltres.size() + " dossier(s) affiché(s)");
    }

    @FXML
    public void effacerFiltreRapide() {
        txtFiltreRapide.clear();
        cbFiltreSexe.setValue("Tous sexes");
    }

    private void configurerDoubleClicTables() {
        // Double-clic sur le registre général -> Ouvre la fiche complète
        tableListeNaissances.setRowFactory(tv -> {
            TableRow<DossierNaissance> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    dossierActuel = row.getItem();
                    afficherVueFiche();
                }
            });
            return row;
        });

        // Double-clic sur le tableau de bord
        tableDerniersDossiers.setRowFactory(tv -> {
            TableRow<DossierNaissance> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    dossierActuel = row.getItem();
                    afficherVueFiche();
                }
            });
            return row;
        });

        // Double-clic sur les résultats de recherche
        tableResultatsRecherche.setRowFactory(tv -> {
            TableRow<DossierNaissance> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    dossierActuel = row.getItem();
                    afficherVueFiche();
                }
            });
            return row;
        });
    }

    // =========================================================================
    // SYNCHRONISATION SQLite & TABLEAU DE BORD
    // =========================================================================

    @FXML
    public void actualiserDonnees() {
        List<DossierNaissance> liste = enfantDAO.listerDossiersComplets();
        tousLesDossiers.setAll(liste);

        // Actualisation des derniers dossiers
        int limite = Math.min(liste.size(), 8);
        derniersDossiers.setAll(liste.subList(0, limite));

        lblTotalActes.setText("Total enregistré : " + liste.size() + " dossier(s)");
        lblCompteurListe.setText(liste.size() + " dossier(s)");

        mettreAJourDashboard();
    }

    private void mettreAJourDashboard() {
        int total = enfantDAO.compter();
        int garcons = enfantDAO.compterParSexe("M");
        int filles = enfantDAO.compterParSexe("F");
        int anneeEnCours = LocalDate.now().getYear();
        int naissancesAnnee = enfantDAO.compterParAnnee(anneeEnCours);

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiGarcons.setText(String.valueOf(garcons));
        lblKpiFilles.setText(String.valueOf(filles));
        lblKpiAnneeTitre.setText("ANNÉE " + anneeEnCours);
        lblKpiAnnee.setText(String.valueOf(naissancesAnnee));

        // Mise à jour du graphique par année
        chartNaissancesParAnnee.getData().clear();
        Map<String, Integer> statsAnnees = enfantDAO.statistiquesParAnnee();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Naissances par an");

        if (statsAnnees.isEmpty()) {
            series.getData().add(new XYChart.Data<>(String.valueOf(anneeEnCours), 0));
        } else {
            for (Map.Entry<String, Integer> entry : statsAnnees.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }
        chartNaissancesParAnnee.getData().add(series);
    }

    @FXML
    public void verifierStatutBase() {
        boolean connectee = DatabaseManager.testConnection();
        if (connectee) {
            lblStatutDB.setText("● Base SQLite connectée (naissances.db)");
            lblStatutDB.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            lblStatutDB.setText("● Base SQLite déconnectée");
            lblStatutDB.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
    }

    // =========================================================================
    // ENREGISTREMENT D'UN DOSSIER COMPLET (7 TABLES RELATIONNELLES)
    // =========================================================================

    @FXML
    public void reinitialiserFormulaireComplet() {
        LocalDate aujourdhui = LocalDate.now();
        int annee = aujourdhui.getYear();

        txtFormAnneeRegistre.setText(String.valueOf(annee));
        txtFormFolio.setText("F-01");
        txtFormTome.setText("T-01");
        dpFormDateDeclaration.setValue(aujourdhui);
        txtFormHeureDeclaration.setText("09:30");

        txtFormIdentifiantUnique.setText(genererIdentifiantUniqueCommunal(annee));
        txtFormNumeroRegistre.setText(genererNumeroRegistre(annee));

        txtFormNomEnfant.clear();
        txtFormPrenomsEnfant.clear();
        cbFormSexe.setValue("Masculin (M)");
        cbFormStatutEnfant.setValue("Enfant");
        dpFormDateNaissance.setValue(aujourdhui);
        txtFormHeureNaissance.setText("08:00");
        txtFormLieuNaissance.setText("Maternité Municipale");

        txtFormNomPere.clear();
        txtFormPrenomPere.clear();
        dpFormDateNaissPere.setValue(null);
        txtFormAgePere.clear();
        txtFormProfessionPere.clear();
        txtFormDomicilePere.clear();

        txtFormNomMere.clear();
        txtFormPrenomMere.clear();
        dpFormDateNaissMere.setValue(null);
        txtFormAgeMere.clear();
        txtFormProfessionMere.clear();
        txtFormDomicileMere.clear();

        txtFormNomDeclarant.clear();
        cbFormQualiteDeclarant.setValue("Père");
        txtFormTemoin.clear();
        txtFormObservations.clear();

        txtFormOfficier.setText("Officier de l'État Civil");
        txtFormFonction.setText("Adjoint au Maire délégué");
        cbFormStatutValidation.setValue("VALIDE");

        cbFormTypeDocument.setValue("Extrait d'acte de naissance");
        txtFormNumeroDocument.setText("DOC-" + annee + "-" + String.format("%04d", tousLesDossiers.size() + 1));
        dpFormDateDelivrance.setValue(aujourdhui);
        txtFormHeureDelivrance.setText("10:00");
        txtFormPersonneReception.clear();
        chkFormConfirmationLivraison.setSelected(true);

        afficherMessageInfo("Formulaire prêt pour une nouvelle déclaration.", false);
    }

    @FXML
    public void genererNouvelIdentifiant() {
        int annee = LocalDate.now().getYear();
        try {
            annee = Integer.parseInt(txtFormAnneeRegistre.getText().trim());
        } catch (Exception ignored) {}
        txtFormIdentifiantUnique.setText(genererIdentifiantUniqueCommunal(annee));
        txtFormNumeroRegistre.setText(genererNumeroRegistre(annee));
    }

    private String genererIdentifiantUniqueCommunal(int annee) {
        return enfantDAO.genererProchainIdentifiantCommunal(annee);
    }

    private String genererNumeroRegistre(int annee) {
        int sequence = enfantDAO.compter() + 1;
        while (enfantDAO.existeNumeroRegistre(String.format("REG-%d-%04d", annee, sequence))) {
            sequence++;
        }
        return String.format("REG-%d-%04d", annee, sequence);
    }

    @FXML
    public void enregistrerDossierComplet() {
        // Contrôle de permission RBAC
        if (!SessionUtilisateur.getInstance().aPermission(Permission.ENREGISTRER_NAISSANCE)) {
            afficherAlerte("Accès Refusé", "Permissions insuffisantes",
                    "Votre rôle ne vous autorise pas à enregistrer une déclaration de naissance.",
                    Alert.AlertType.WARNING);
            return;
        }

        // 1. Validation des champs obligatoires de l'enfant
        String idUnique = txtFormIdentifiantUnique.getText() != null ? txtFormIdentifiantUnique.getText().trim() : "";
        String numRegistre = txtFormNumeroRegistre.getText() != null ? txtFormNumeroRegistre.getText().trim() : "";
        String nomEnfant = txtFormNomEnfant.getText() != null ? txtFormNomEnfant.getText().trim().toUpperCase() : "";
        String prenomsEnfant = txtFormPrenomsEnfant.getText() != null ? txtFormPrenomsEnfant.getText().trim() : "";
        String sexeChoisi = cbFormSexe.getValue();
        LocalDate dateNaissPicker = dpFormDateNaissance.getValue();
        String lieuNaiss = txtFormLieuNaissance.getText() != null ? txtFormLieuNaissance.getText().trim() : "";

        if (idUnique.isEmpty()) {
            afficherAlerte("Champ obligatoire", "L'identifiant unique communal est obligatoire.");
            return;
        }
        if (numRegistre.isEmpty()) {
            afficherAlerte("Champ obligatoire", "Le numéro de registre est obligatoire.");
            return;
        }
        if (nomEnfant.isEmpty()) {
            afficherAlerte("Champ obligatoire", "Veuillez renseigner le nom de l'enfant.");
            return;
        }
        if (prenomsEnfant.isEmpty()) {
            afficherAlerte("Champ obligatoire", "Veuillez renseigner les prénoms de l'enfant.");
            return;
        }
        if (sexeChoisi == null || sexeChoisi.trim().isEmpty()) {
            afficherAlerte("Champ obligatoire", "Veuillez sélectionner le sexe.");
            return;
        }
        if (dateNaissPicker == null) {
            afficherAlerte("Champ obligatoire", "La date de naissance est obligatoire.");
            return;
        }
        if (lieuNaiss.isEmpty()) {
            afficherAlerte("Champ obligatoire", "Le lieu de naissance est obligatoire.");
            return;
        }

        // 2. Contrôle préalable des doublons
        if (enfantDAO.existeNumeroRegistre(numRegistre)) {
            afficherAlerte("Doublon détecté", "Le numéro de registre existe déjà.");
            return;
        }
        if (enfantDAO.existeIdentifiantUnique(idUnique)) {
            afficherAlerte("Doublon détecté", "L'identifiant unique communal existe déjà.");
            return;
        }

        int anneeReg = LocalDate.now().getYear();
        try {
            anneeReg = Integer.parseInt(txtFormAnneeRegistre.getText().trim());
        } catch (Exception ignored) {}

        String numDeclaration = "DECL-" + anneeReg + "-" + numRegistre.replaceAll("[^0-9]", "");
        if (numDeclaration.length() <= 10) {
            numDeclaration = "DECL-" + anneeReg + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
        }
        if (declarationDAO.existeNumeroDeclaration(numDeclaration)) {
            afficherAlerte("Doublon détecté", "Le numéro de déclaration existe déjà.");
            return;
        }

        // 3. Construction des entités pour la transaction
        String sexe = sexeChoisi.startsWith("F") ? "F" : "M";
        String dateNaiss = dateNaissPicker.format(dateFormatter);

        Enfant enfant = new Enfant();
        enfant.setIdentifiantUnique(idUnique);
        enfant.setNumeroRegistre(numRegistre);
        enfant.setNom(nomEnfant);
        enfant.setPrenoms(prenomsEnfant);
        enfant.setSexe(sexe);
        enfant.setDateNaissance(dateNaiss);
        enfant.setHeureNaissance(txtFormHeureNaissance.getText() != null ? txtFormHeureNaissance.getText().trim() : "");
        enfant.setLieuNaissance(lieuNaiss);
        enfant.setStatut(cbFormStatutEnfant.getValue() != null ? cbFormStatutEnfant.getValue() : "Enfant");

        // Informations des parents
        ParentInfo parents = new ParentInfo();
        parents.setNomPere(txtFormNomPere.getText() != null ? txtFormNomPere.getText().trim().toUpperCase() : "");
        parents.setPrenomPere(txtFormPrenomPere.getText() != null ? txtFormPrenomPere.getText().trim() : "");
        String dateNaissPere = dpFormDateNaissPere.getValue() != null ? dpFormDateNaissPere.getValue().format(dateFormatter) : null;
        parents.setDateNaissancePere(dateNaissPere);
        if (dateNaissPere != null && !dateNaissPere.isBlank()) {
            parents.setAgePere(ParentInfo.calculerAge(dateNaissPere));
        } else {
            try { parents.setAgePere(Integer.parseInt(txtFormAgePere.getText().trim())); } catch (Exception e) { parents.setAgePere(0); }
        }
        parents.setProfessionPere(txtFormProfessionPere.getText() != null ? txtFormProfessionPere.getText().trim() : "");
        parents.setDomicilePere(txtFormDomicilePere.getText() != null ? txtFormDomicilePere.getText().trim() : "");

        parents.setNomMere(txtFormNomMere.getText() != null ? txtFormNomMere.getText().trim().toUpperCase() : "");
        parents.setPrenomMere(txtFormPrenomMere.getText() != null ? txtFormPrenomMere.getText().trim() : "");
        String dateNaissMere = dpFormDateNaissMere.getValue() != null ? dpFormDateNaissMere.getValue().format(dateFormatter) : null;
        parents.setDateNaissanceMere(dateNaissMere);
        if (dateNaissMere != null && !dateNaissMere.isBlank()) {
            parents.setAgeMere(ParentInfo.calculerAge(dateNaissMere));
        } else {
            try { parents.setAgeMere(Integer.parseInt(txtFormAgeMere.getText().trim())); } catch (Exception e) { parents.setAgeMere(0); }
        }
        parents.setProfessionMere(txtFormProfessionMere.getText() != null ? txtFormProfessionMere.getText().trim() : "");
        parents.setDomicileMere(txtFormDomicileMere.getText() != null ? txtFormDomicileMere.getText().trim() : "");

        // Déclaration
        Declaration decl = new Declaration();
        decl.setNumeroDeclaration(numDeclaration);
        decl.setAnneeDeclaration(anneeReg);
        decl.setDateDeclaration(dpFormDateDeclaration.getValue() != null ? dpFormDateDeclaration.getValue().format(dateFormatter) : LocalDate.now().format(dateFormatter));
        decl.setHeureDeclaration(txtFormHeureDeclaration.getText() != null ? txtFormHeureDeclaration.getText().trim() : "09:30");
        String nomDeclarant = txtFormNomDeclarant.getText() != null ? txtFormNomDeclarant.getText().trim() : "";
        decl.setNomDeclarant(!nomDeclarant.isEmpty() ? nomDeclarant : (!parents.getNomPere().isEmpty() ? parents.getNomPere() + " " + parents.getPrenomPere() : "Déclarant légal"));
        decl.setQualiteDeclarant(cbFormQualiteDeclarant.getValue() != null ? cbFormQualiteDeclarant.getValue() : "Père");
        decl.setObservations(txtFormObservations.getText() != null ? txtFormObservations.getText().trim() : "");

        // Enregistrement communal
        EnregistrementCommune commune = new EnregistrementCommune();
        commune.setNomResponsable(txtFormOfficier.getText() != null ? txtFormOfficier.getText().trim() : "Officier de l'État Civil");
        commune.setFonctionResponsable(txtFormFonction.getText() != null ? txtFormFonction.getText().trim() : "Officier délégué");
        commune.setDateEnregistrement(LocalDate.now().format(dateFormatter));
        commune.setHeureEnregistrement("09:30");
        commune.setFolio(txtFormFolio.getText() != null ? txtFormFolio.getText().trim() : "F-01");
        commune.setTome(txtFormTome.getText() != null ? txtFormTome.getText().trim() : "T-01");
        commune.setStatutValidation(cbFormStatutValidation.getValue() != null ? cbFormStatutValidation.getValue() : "VALIDE");

        // Document délivré
        DocumentDelivre doc = new DocumentDelivre();
        doc.setTypeDocument(cbFormTypeDocument.getValue() != null ? cbFormTypeDocument.getValue() : "Extrait d'acte");
        String numDoc = txtFormNumeroDocument.getText() != null ? txtFormNumeroDocument.getText().trim() : "";
        doc.setNumeroDocument(!numDoc.isEmpty() ? numDoc : "DOC-" + anneeReg + "-" + numRegistre);
        doc.setDateDelivrance(dpFormDateDelivrance.getValue() != null ? dpFormDateDelivrance.getValue().format(dateFormatter) : LocalDate.now().format(dateFormatter));
        doc.setHeureDelivrance(txtFormHeureDelivrance.getText() != null ? txtFormHeureDelivrance.getText().trim() : "10:00");
        String persRec = txtFormPersonneReception.getText() != null ? txtFormPersonneReception.getText().trim() : "";
        doc.setNomPersonneReception(!persRec.isEmpty() ? persRec : decl.getNomDeclarant());
        doc.setQualitePersonneReception(decl.getQualiteDeclarant());
        doc.setConfirmationLivraison(chkFormConfirmationLivraison.isSelected());

        // Création du DossierNaissance
        DossierNaissance dossier = new DossierNaissance(enfant, parents, decl, commune, doc);

        // 4. Enregistrement transactionnel dans SQLite avec COMMIT/ROLLBACK
        try {
            boolean reussi = enfantDAO.enregistrerDossierTransactionnel(dossier);
            if (!reussi) {
                afficherAlerte("Erreur", "Impossible d'enregistrer le dossier. Les données n'ont pas été modifiées.");
                return;
            }

            // 5. Actualisation immédiate du registre, de la recherche et du tableau de bord
            actualiserDonnees();
            mettreAJourDashboard();
            dossierActuel = enfantDAO.chargerDossierComplet(enfant.getId());

            // 6. Dialogue de confirmation conforme
            Alert dialogueSucces = new Alert(Alert.AlertType.INFORMATION);
            dialogueSucces.setTitle("Enregistrement réussi");
            dialogueSucces.setHeaderText("Le dossier de naissance a été enregistré avec succès.");
            dialogueSucces.setContentText(
                    "Identifiant unique communal : " + idUnique + "\n" +
                    "Numéro de registre : " + numRegistre + "\n" +
                    "Enfant : " + nomEnfant + " " + prenomsEnfant + "\n\n" +
                    "Toutes les informations ont été enregistrées avec succès dans les 7 tables SQLite."
            );

            ButtonType btnConsulter = new ButtonType("Consulter le dossier", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNouveau = new ButtonType("Nouveau dossier", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialogueSucces.getButtonTypes().setAll(btnConsulter, btnNouveau);

            Optional<ButtonType> choix = dialogueSucces.showAndWait();
            reinitialiserFormulaireComplet();

            if (choix.isPresent() && choix.get() == btnConsulter) {
                afficherVueFiche();
            } else {
                afficherVueFormulaire();
            }
            afficherMessageInfo("Dossier " + numRegistre + " enregistré dans SQLite.", false);

        } catch (ValidationException ve) {
            afficherAlerte("Erreur de validation", ve.getMessage());
            afficherMessageInfo(ve.getMessage(), true);
        } catch (Exception ex) {
            System.err.println("[MainController] Échec transaction enregistrement : " + ex.getMessage());
            ex.printStackTrace();
            afficherAlerte("Erreur d'enregistrement", "Impossible d'enregistrer le dossier. Les données n'ont pas été modifiées.");
            afficherMessageInfo("Impossible d'enregistrer le dossier. Les données n'ont pas été modifiées.", true);
        }
    }

    // =========================================================================
    // CONSULTATION DE LA FICHE COMPLÈTE
    // =========================================================================

    private void remplirFicheOfficielle(DossierNaissance d) {
        if (d == null) return;

        Enfant e = d.getEnfant();
        ParentInfo p = d.getParents();
        Declaration dec = d.getDeclaration();
        EnregistrementCommune c = d.getCommune();
        DocumentDelivre doc = d.getDocument();

        // En-tête
        lblFicheIdentifiantUnique.setText(e != null && e.getIdentifiantUnique() != null ? e.getIdentifiantUnique() : "NON ATTRIBUÉ");
        lblFicheNumeroRegistre.setText(e != null && e.getNumeroRegistre() != null ? e.getNumeroRegistre() : "-");

        // Enfant
        lblFicheNom.setText(e != null ? e.getNom() : "");
        lblFichePrenoms.setText(e != null ? e.getPrenoms() : "");
        lblFicheSexe.setText(e != null ? ("F".equalsIgnoreCase(e.getSexe()) ? "Féminin (F)" : "Masculin (M)") : "");
        lblFicheDateHeureNaiss.setText((e != null ? formaterDate(e.getDateNaissance()) : "") +
                (e != null && e.getHeureNaissance() != null && !e.getHeureNaissance().isEmpty() ? " à " + e.getHeureNaissance() : ""));
        lblFicheLieuNaiss.setText(e != null ? e.getLieuNaissance() : "");
        lblFicheStatutEnfant.setText(e != null ? e.getStatut() : "Enfant");

        // Parents
        if (p != null) {
            lblFicheNomPrenomPere.setText((p.getPrenomPere() != null ? p.getPrenomPere() + " " : "") + (p.getNomPere() != null ? p.getNomPere() : "Non renseigné"));
            lblFicheNaissAgePere.setText("Né le : " + formaterDate(p.getDateNaissancePere()) + (p.getAgePere() > 0 ? " (" + p.getAgePere() + " ans)" : ""));
            lblFicheProfPere.setText("Profession : " + (p.getProfessionPere() != null && !p.getProfessionPere().isEmpty() ? p.getProfessionPere() : "Non renseignée"));
            lblFicheDomPere.setText("Domicile : " + (p.getDomicilePere() != null && !p.getDomicilePere().isEmpty() ? p.getDomicilePere() : "Non renseigné"));

            lblFicheNomPrenomMere.setText((p.getPrenomMere() != null ? p.getPrenomMere() + " " : "") + (p.getNomMere() != null ? p.getNomMere() : ""));
            lblFicheNaissAgeMere.setText("Née le : " + formaterDate(p.getDateNaissanceMere()) + (p.getAgeMere() > 0 ? " (" + p.getAgeMere() + " ans)" : ""));
            lblFicheProfMere.setText("Profession : " + (p.getProfessionMere() != null && !p.getProfessionMere().isEmpty() ? p.getProfessionMere() : "Non renseignée"));
            lblFicheDomMere.setText("Domicile : " + (p.getDomicileMere() != null && !p.getDomicileMere().isEmpty() ? p.getDomicileMere() : "Non renseigné"));
        }

        // Déclaration & Enregistrement
        if (dec != null) {
            lblFicheDeclarant.setText(dec.getNomDeclarant() + (dec.getQualiteDeclarant() != null ? " (" + dec.getQualiteDeclarant() + ")" : ""));
            lblFicheDateDeclaration.setText(formaterDate(dec.getDateDeclaration()) + (dec.getHeureDeclaration() != null && !dec.getHeureDeclaration().isEmpty() ? " à " + dec.getHeureDeclaration() : ""));
            lblFicheObservations.setText(dec.getObservations() != null && !dec.getObservations().isEmpty() ? dec.getObservations() : "Néant");
        }
        if (c != null) {
            lblFicheOfficier.setText(c.getNomResponsable() != null ? c.getNomResponsable() + " (" + c.getFonctionResponsable() + ")" : "Officier de l'État Civil");
            lblFicheTomeFolio.setText("Tome : " + (c.getTome() != null ? c.getTome() : "T-01") + " | Folio : " + (c.getFolio() != null ? c.getFolio() : "F-01"));
            lblFicheStatutValidation.setText(c.getStatutValidation() != null ? c.getStatutValidation() : "VALIDE");
        }

        // Documents
        if (doc != null) {
            lblFicheTypeDoc.setText(doc.getTypeDocument() != null ? doc.getTypeDocument() : "Extrait d'acte de naissance");
            lblFicheNumDoc.setText(doc.getNumeroDocument() != null ? doc.getNumeroDocument() : "-");
            lblFicheDateDoc.setText(formaterDate(doc.getDateDelivrance()) + (doc.getHeureDelivrance() != null && !doc.getHeureDelivrance().isEmpty() ? " à " + doc.getHeureDelivrance() : ""));
            lblFicheReceptionnaire.setText(doc.getNomPersonneReception() != null ? doc.getNomPersonneReception() : "Déclarant");
            lblFicheRemiseConfirmation.setText(doc.isConfirmationLivraison() ? "✓ Confirmée en main propre" : "En cours de délivrance");
        }
    }

    private String formaterDate(String dateIso) {
        if (dateIso == null || dateIso.isBlank()) return "—";
        try {
            LocalDate d = LocalDate.parse(dateIso, dateFormatter);
            return d.format(affichageDateFormatter);
        } catch (Exception e) {
            return dateIso;
        }
    }

    @FXML
    public void consulterFicheSelectionnee() {
        DossierNaissance selection = tableListeNaissances.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte("Aucune sélection", "Veuillez d'abord sélectionner un dossier dans le registre.");
            return;
        }
        dossierActuel = selection;
        afficherVueFiche();
    }

    @FXML
    public void consulterFicheDepuisRecherche() {
        DossierNaissance selection = tableResultatsRecherche.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte("Aucune sélection", "Veuillez sélectionner un résultat de recherche.");
            return;
        }
        dossierActuel = selection;
        afficherVueFiche();
    }

    @FXML
    public void chargerDossierEnModification() {
        if (dossierActuel == null) return;
        Enfant e = dossierActuel.getEnfant();
        ParentInfo p = dossierActuel.getParents();
        Declaration dec = dossierActuel.getDeclaration();
        EnregistrementCommune c = dossierActuel.getCommune();
        DocumentDelivre doc = dossierActuel.getDocument();

        if (e != null) {
            txtFormIdentifiantUnique.setText(e.getIdentifiantUnique());
            txtFormNumeroRegistre.setText(e.getNumeroRegistre());
            txtFormNomEnfant.setText(e.getNom());
            txtFormPrenomsEnfant.setText(e.getPrenoms());
            cbFormSexe.setValue("F".equalsIgnoreCase(e.getSexe()) ? "Féminin (F)" : "Masculin (M)");
            cbFormStatutEnfant.setValue(e.getStatut());
            try { dpFormDateNaissance.setValue(LocalDate.parse(e.getDateNaissance(), dateFormatter)); } catch (Exception ignored) {}
            txtFormHeureNaissance.setText(e.getHeureNaissance());
            txtFormLieuNaissance.setText(e.getLieuNaissance());
        }

        if (p != null) {
            txtFormNomPere.setText(p.getNomPere());
            txtFormPrenomPere.setText(p.getPrenomPere());
            try { dpFormDateNaissPere.setValue(LocalDate.parse(p.getDateNaissancePere(), dateFormatter)); } catch (Exception ignored) {}
            txtFormAgePere.setText(String.valueOf(p.getAgePere()));
            txtFormProfessionPere.setText(p.getProfessionPere());
            txtFormDomicilePere.setText(p.getDomicilePere());

            txtFormNomMere.setText(p.getNomMere());
            txtFormPrenomMere.setText(p.getPrenomMere());
            try { dpFormDateNaissMere.setValue(LocalDate.parse(p.getDateNaissanceMere(), dateFormatter)); } catch (Exception ignored) {}
            txtFormAgeMere.setText(String.valueOf(p.getAgeMere()));
            txtFormProfessionMere.setText(p.getProfessionMere());
            txtFormDomicileMere.setText(p.getDomicileMere());
        }

        if (dec != null) {
            txtFormNomDeclarant.setText(dec.getNomDeclarant());
            cbFormQualiteDeclarant.setValue(dec.getQualiteDeclarant());
            txtFormObservations.setText(dec.getObservations());
        }

        afficherVueFormulaire();
        afficherMessageInfo("Dossier " + (e != null ? e.getNumeroRegistre() : "") + " chargé dans le formulaire.", false);
    }

    @FXML
    public void supprimerDossierSelectionne() {
        DossierNaissance selection = tableListeNaissances.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte("Aucune sélection", "Veuillez sélectionner un dossier à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer le dossier " + selection.getNumeroRegistre() + " ?");
        confirmation.setContentText("Attention : Cette action retirera définitivement ce dossier et ses enregistrements liés (parents, déclaration, document) de la base locale.");

        Optional<ButtonType> rep = confirmation.showAndWait();
        if (rep.isPresent() && rep.get() == ButtonType.OK) {
            boolean supprime = enfantDAO.supprimer(selection.getId());
            if (supprime) {
                actualiserDonnees();
                afficherMessageInfo("Dossier supprimé de la base SQLite.", false);
            } else {
                afficherAlerte("Erreur", "La suppression n'a pas pu aboutir.");
            }
        }
    }

    // =========================================================================
    // RECHERCHE MULTICRITÈRES
    // =========================================================================

    @FXML
    public void executerRechercheMulticriteres() {
        String idU = txtSearchIdentifiant.getText();
        String numReg = txtSearchNumRegistre.getText();
        String nom = txtSearchNomEnfant.getText();
        String prenom = txtSearchPrenomsEnfant.getText();
        String nomP = txtSearchNomPere.getText();
        String nomM = txtSearchNomMere.getText();
        String anneeN = txtSearchAnneeNaissance.getText();
        String anneeD = txtSearchAnneeDeclaration.getText();

        List<DossierNaissance> resultats = enfantDAO.rechercherMultiCriteres(
                idU, numReg, nom, prenom, nomP, nomM, anneeN, anneeD
        );

        resultatsRecherche.setAll(resultats);
        lblResultatsRechercheCount.setText(resultats.size() + " dossier(s) trouvé(s)");
        afficherMessageInfo("Recherche terminée : " + resultats.size() + " résultat(s).", false);
    }

    @FXML
    public void reinitialiserCriteresRecherche() {
        txtSearchIdentifiant.clear();
        txtSearchNumRegistre.clear();
        txtSearchNomEnfant.clear();
        txtSearchPrenomsEnfant.clear();
        txtSearchNomPere.clear();
        txtSearchNomMere.clear();
        txtSearchAnneeNaissance.clear();
        txtSearchAnneeDeclaration.clear();
        resultatsRecherche.clear();
        lblResultatsRechercheCount.setText("Critères réinitialisés");
    }

    // =========================================================================
    // IMPRESSION & EXTRAITS OFFICIELS
    // =========================================================================

    @FXML
    public void imprimerExtraitActuel() {
        if (dossierActuel == null) {
            afficherAlerte("Aucun dossier", "Aucun dossier n'est actuellement affiché.");
            return;
        }
        genererApercuImpression(dossierActuel);
    }

    @FXML
    public void imprimerExtraitDepuisSelection() {
        DossierNaissance selection = tableListeNaissances.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte("Aucune sélection", "Veuillez d'abord sélectionner un acte de naissance dans le tableau.");
            return;
        }
        genererApercuImpression(selection);
    }

    private void genererApercuImpression(DossierNaissance d) {
        Alert dialogue = new Alert(Alert.AlertType.INFORMATION);
        dialogue.setTitle("Édition de l'Extrait d'Acte de Naissance");
        dialogue.setHeaderText("RÉPUBLIQUE - EXTRAIT DU REGISTRE DE L'ÉTAT CIVIL");
        dialogue.setContentText(
                "====================================================\n" +
                "EXTRAIT D'ACTE DE NAISSANCE OFFICIEL\n" +
                "====================================================\n\n" +
                "Identifiant unique communal : " + d.getIdentifiantUnique() + "\n" +
                "Numéro au Registre : " + d.getNumeroRegistre() + "\n\n" +
                "Le " + formaterDate(d.getDateNaissance()) + "\n" +
                "Est né(e) à : " + d.getLieuNaissance() + "\n" +
                "L'enfant de sexe : " + ("F".equalsIgnoreCase(d.getSexe()) ? "Féminin" : "Masculin") + "\n" +
                "Nom : " + d.getNom() + "\n" +
                "Prénoms : " + d.getPrenoms() + "\n\n" +
                "Filiation :\n" +
                "— Père : " + d.getNomPere() + "\n" +
                "— Mère : " + d.getNomMere() + "\n\n" +
                "Délivré par le Service de l'État Civil de la Commune.\n" +
                "Certifié conforme au registre déposé en mairie."
        );
        dialogue.showAndWait();
    }

    @FXML
    public void afficherAPropos() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("À propos");
        a.setHeaderText("Gestion des Naissances — Version 1.3");
        a.setContentText("Application desktop locale et autonome d'archivage numérique des actes d'état civil.\n\n" +
                "• Architecture : Java 17, JavaFX, SQLite JDBC\n" +
                "• Base relationnelle : naissances.db (7 tables relationnelles)\n" +
                "• Mode : 100% Hors-Ligne pour une administration communale souveraine.");
        a.showAndWait();
    }

    @FXML
    public void quitterApplication() {
        Platform.exit();
    }

    // =========================================================================
    // UTILITAIRES D'AFFICHAGE ET FEEDBACK
    // =========================================================================

    private void afficherAlerte(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        alerte.showAndWait();
    }

    private void afficherAlerte(String titre, String entete, String message, Alert.AlertType type) {
        Alert alerte = new Alert(type);
        alerte.setTitle(titre);
        alerte.setHeaderText(entete);
        alerte.setContentText(message);
        alerte.showAndWait();
    }

    private void afficherMessageInfo(String message, boolean erreur) {
        lblMessageInfo.setText(message);
        lblMessageInfo.setStyle(erreur ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #f1f5f9;");
    }

    // =========================================================================
    // ÉTAPE 5 : LOGIQUE DE SESSION, RBAC ET SÉCURITÉ UTILISATEURS
    // =========================================================================

    private void configurerSessionEtRoles() {
        if (!SessionUtilisateur.estConnecte()) {
            System.out.println("[Sécurité] Aucune session active. Connexion par défaut (ADMINISTRATEUR).");
            SessionUtilisateur.ouvrirSession(1, "admin", "Administrateur Principal", Role.ADMINISTRATEUR);
        }

        SessionUtilisateur session = SessionUtilisateur.getInstance();
        Role role = session.getRole();

        if (lblHeaderUtilisateur != null) {
            lblHeaderUtilisateur.setText(session.getNomComplet() + " (" + role.getLibelle() + ")");
        }
        if (lblSidebarNomUtilisateur != null) {
            lblSidebarNomUtilisateur.setText(session.getNomComplet());
        }
        if (lblSidebarRoleBadge != null) {
            lblSidebarRoleBadge.setText(role.name());
            lblSidebarRoleBadge.getStyleClass().removeAll("role-chip-admin", "role-chip-responsable", "role-chip-agent");
            switch (role) {
                case ADMINISTRATEUR -> lblSidebarRoleBadge.getStyleClass().add("role-chip-admin");
                case RESPONSABLE_COMMUNAL -> lblSidebarRoleBadge.getStyleClass().add("role-chip-responsable");
                case AGENT -> lblSidebarRoleBadge.getStyleClass().add("role-chip-agent");
            }
        }

        // Contrôle d'accès RBAC sur la Sidebar :
        // 1. ADMINISTRATEUR : voit la gestion des utilisateurs
        // 2. RESPONSABLE_COMMUNAL & AGENT : masquent et bloquent la gestion des utilisateurs
        if (btnNavUtilisateurs != null) {
            boolean autoriseUtilisateurs = session.aPermission(Permission.GERER_UTILISATEURS);
            btnNavUtilisateurs.setVisible(autoriseUtilisateurs);
            btnNavUtilisateurs.setManaged(autoriseUtilisateurs);
        }
    }

    private void chargerListeUtilisateurs() {
        listeUtilisateurs.clear();
        listeUtilisateurs.addAll(utilisateurDAO.listerTous());
    }

    @FXML
    public void creerNouvelUtilisateur() {
        if (!SessionUtilisateur.getInstance().aPermission(Permission.GERER_UTILISATEURS)) {
            afficherAlerte("Accès Refusé", "Action non autorisée",
                    "Vous ne disposez pas des privilèges nécessaires pour créer un compte utilisateur.",
                    Alert.AlertType.ERROR);
            return;
        }

        String login = txtNewUserLogin != null ? txtNewUserLogin.getText().trim() : "";
        String nomComplet = txtNewUserNomComplet != null ? txtNewUserNomComplet.getText().trim() : "";
        String roleStr = cbNewUserRole != null ? cbNewUserRole.getValue() : "AGENT";
        String mdp = txtNewUserMdp != null ? txtNewUserMdp.getText() : "";

        if (login.isEmpty() || mdp.isEmpty() || nomComplet.isEmpty()) {
            afficherAlerte("Champs Incomplets", "Saisie obligatoire",
                    "Veuillez renseigner le nom d'utilisateur, le nom complet et le mot de passe initial.",
                    Alert.AlertType.WARNING);
            return;
        }

        if (mdp.length() < 6) {
            afficherAlerte("Mot de passe trop court", "Contrainte de sécurité",
                    "Le mot de passe initial doit contenir au moins 6 caractères.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            String hash = PasswordService.hasher(mdp);
            Utilisateur nouvelUtilisateur = new Utilisateur(login, hash, nomComplet, roleStr, true);
            boolean ok = utilisateurDAO.inserer(nouvelUtilisateur);
            if (ok) {
                afficherAlerte("Compte Créé", "Utilisateur enregistré avec succès",
                        "Le compte « " + login + " » a été créé dans la base SQLite locale.\nMot de passe protégé par PBKDF2-HMAC-SHA256.",
                        Alert.AlertType.INFORMATION);
                if (txtNewUserLogin != null) txtNewUserLogin.clear();
                if (txtNewUserNomComplet != null) txtNewUserNomComplet.clear();
                if (txtNewUserMdp != null) txtNewUserMdp.clear();
                chargerListeUtilisateurs();
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Échec d'enregistrement",
                    "Erreur lors de la création de l'utilisateur : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void toggleActifUtilisateur() {
        if (!SessionUtilisateur.getInstance().aPermission(Permission.GERER_UTILISATEURS)) {
            return;
        }
        Utilisateur selectionne = tableUtilisateurs != null ? tableUtilisateurs.getSelectionModel().getSelectedItem() : null;
        if (selectionne == null) {
            afficherAlerte("Sélection requise", "Veuillez sélectionner un compte d'utilisateur dans le tableau.");
            return;
        }
        if (selectionne.getId() == SessionUtilisateur.getInstance().getId()) {
            afficherAlerte("Action Interdite", "Sécurité de session",
                    "Vous ne pouvez pas désactiver le compte avec lequel vous êtes actuellement connecté.",
                    Alert.AlertType.WARNING);
            return;
        }

        boolean nouveauStatut = !selectionne.isActif();
        utilisateurDAO.setStatutActif(selectionne.getId(), nouveauStatut);
        chargerListeUtilisateurs();
        afficherMessageInfo("Statut du compte « " + selectionne.getNomUtilisateur() + " » : " 
                + (nouveauStatut ? "Activé" : "Désactivé"), false);
    }

    @FXML
    public void seDeconnecter(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Déconnexion");
        confirmation.setHeaderText("Fermeture de session");
        confirmation.setContentText("Êtes-vous certain de vouloir vous déconnecter du registre d'état civil ?");
        Optional<ButtonType> rep = confirmation.showAndWait();
        if (rep.isPresent() && rep.get() == ButtonType.OK) {
            SessionUtilisateur.deconnexion();
            System.out.println("[Sécurité] Session terminée par l'utilisateur.");

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestionnaissances/view/login-view.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 1000, 700);

                String cssPath = getClass().getResource("/com/gestionnaissances/css/style.css") != null
                        ? getClass().getResource("/com/gestionnaissances/css/style.css").toExternalForm() : null;
                if (cssPath != null) {
                    scene.getStylesheets().add(cssPath);
                }

                stage.setTitle("Gestion des Naissances — Connexion Sécurisée");
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();
            } catch (IOException ex) {
                System.err.println("[MainController] Erreur lors de la déconnexion : " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @FXML
    public void ouvrirDialogueChangementMotDePasse() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Sécurité du Compte");
        dialog.setHeaderText("Changer mon mot de passe de connexion");

        ButtonType btnValiderType = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValiderType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        PasswordField pfActuel = new PasswordField();
        pfActuel.setPromptText("Mot de passe actuel");
        PasswordField pfNouveau = new PasswordField();
        pfNouveau.setPromptText("Nouveau mot de passe (min. 6 car.)");
        PasswordField pfConfirmation = new PasswordField();
        pfConfirmation.setPromptText("Confirmer le mot de passe");

        grid.add(new Label("Mot de passe actuel :"), 0, 0);
        grid.add(pfActuel, 1, 0);
        grid.add(new Label("Nouveau mot de passe :"), 0, 1);
        grid.add(pfNouveau, 1, 1);
        grid.add(new Label("Confirmation :"), 0, 2);
        grid.add(pfConfirmation, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnValiderType) {
                try {
                    int currentUserId = SessionUtilisateur.getInstance().getId();
                    authService.changerMotDePasse(currentUserId,
                            pfActuel.getText(),
                            pfNouveau.getText(),
                            pfConfirmation.getText());
                    return true;
                } catch (ValidationException ex) {
                    afficherAlerte("Validation", "Échec du changement de mot de passe",
                            ex.getMessage(), Alert.AlertType.ERROR);
                    return false;
                }
            }
            return null;
        });

        Optional<Boolean> res = dialog.showAndWait();
        if (res.isPresent() && Boolean.TRUE.equals(res.get())) {
            afficherAlerte("Succès", "Mot de passe mis à jour",
                    "Votre mot de passe a été modifié avec succès et son hachage sécurisé mis à jour dans SQLite.",
                    Alert.AlertType.INFORMATION);
        }
    }
}
