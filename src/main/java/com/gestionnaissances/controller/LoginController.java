package com.gestionnaissances.controller;

import com.gestionnaissances.security.AuthentificationResultat;
import com.gestionnaissances.security.AuthentificationService;
import com.gestionnaissances.security.SessionUtilisateur;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur de l'écran de connexion JavaFX de l'application « Gestion des Naissances ».
 * Gère l'authentification sécurisée, la validation des champs,
 * l'affichage des alertes et la redirection vers le tableau de bord principal.
 */
public class LoginController {

    @FXML private TextField txtNomUtilisateur;
    @FXML private PasswordField txtMotDePasse;
    @FXML private Label lblMessageErreur;
    @FXML private Button btnConnexion;
    @FXML private Label lblStatutHorsLigne;

    private final AuthentificationService authService = new AuthentificationService();

    @FXML
    public void initialize() {
        if (lblMessageErreur != null) {
            lblMessageErreur.setText("");
            lblMessageErreur.setVisible(false);
            lblMessageErreur.setManaged(false);
        }

        // Focus automatique sur le champ identifiant
        Platform.runLater(() -> {
            if (txtNomUtilisateur != null) {
                txtNomUtilisateur.requestFocus();
            }
        });
    }

    /**
     * Déclenche la tentative de connexion lors du clic sur le bouton « Se connecter ».
     */
    @FXML
    public void onConnexion(ActionEvent event) {
        validerEtConnecter((Stage) ((Node) event.getSource()).getScene().getWindow());
    }

    /**
     * Permet la soumission rapide par appui sur la touche [Entrée].
     */
    @FXML
    public void onToucheEntree(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            validerEtConnecter((Stage) ((Node) event.getSource()).getScene().getWindow());
        }
    }

    private void validerEtConnecter(Stage stage) {
        String identifiant = txtNomUtilisateur != null ? txtNomUtilisateur.getText() : "";
        String motDePasse = txtMotDePasse != null ? txtMotDePasse.getText() : "";

        if (identifiant.trim().isEmpty() || motDePasse.trim().isEmpty()) {
            afficherErreur("Veuillez saisir votre nom d'utilisateur et votre mot de passe.");
            return;
        }

        // Exécution de l'authentification avec hachage PBKDF2 et contrôle des rôles
        AuthentificationResultat resultat = authService.authentifier(identifiant, motDePasse);

        if (resultat.isSucces()) {
            masquerErreur();
            System.out.println("[Sécurité] Connexion réussie pour l'utilisateur ID="
                    + resultat.getUtilisateur().getId()
                    + " (" + resultat.getUtilisateur().getRole() + ")");

            // Redirection vers le tableau de bord principal
            ouvrirApplicationPrincipale(stage);
        } else {
            // Affichage d'un message clair et sécurisé
            afficherErreur(resultat.getMessage());
            if (txtMotDePasse != null) {
                txtMotDePasse.clear();
                txtMotDePasse.requestFocus();
            }
        }
    }

    /**
     * Charge la vue principale main-view.fxml avec initialisation de la session connectée.
     */
    private void ouvrirApplicationPrincipale(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestionnaissances/view/main-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            String cssPath = getClass().getResource("/com/gestionnaissances/css/style.css") != null
                    ? getClass().getResource("/com/gestionnaissances/css/style.css").toExternalForm()
                    : null;
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            }

            stage.setTitle("Gestion des Naissances — Registre d'État Civil [" 
                    + SessionUtilisateur.getInstance().getRole().getLibelle() + " : " 
                    + SessionUtilisateur.getInstance().getNomComplet() + "]");
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("[LoginController] Erreur lors du chargement de main-view.fxml : " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Erreur système lors de l'ouverture de l'application : " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        if (lblMessageErreur != null) {
            lblMessageErreur.setText(message);
            lblMessageErreur.setVisible(true);
            lblMessageErreur.setManaged(true);
        }
    }

    private void masquerErreur() {
        if (lblMessageErreur != null) {
            lblMessageErreur.setText("");
            lblMessageErreur.setVisible(false);
            lblMessageErreur.setManaged(false);
        }
    }
}
