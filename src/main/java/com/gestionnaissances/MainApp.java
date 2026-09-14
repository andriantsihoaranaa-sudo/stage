package com.gestionnaissances;

import com.gestionnaissances.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principale JavaFX de l'application « Gestion des Naissances ».
 * Initialise le moteur SQLite et charge l'interface graphique de bureau.
 */
public class MainApp extends Application {

    private static final String APP_TITLE = "Gestion des Naissances - Registre d'État Civil";
    private static final double DEFAULT_WIDTH = 1180;
    private static final double DEFAULT_HEIGHT = 760;

    @Override
    public void init() throws Exception {
        super.init();
        // Étape 1 : Initialisation de la base SQLite locale hors-ligne
        System.out.println("[MainApp] Démarrage du système...");
        DatabaseManager.initializeDatabase();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("[MainApp] Chargement de l'écran d'authentification JavaFX...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestionnaissances/view/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 700);

            // Chargement de la feuille de style CSS
            String cssPath = getClass().getResource("/com/gestionnaissances/css/style.css") != null 
                    ? getClass().getResource("/com/gestionnaissances/css/style.css").toExternalForm() 
                    : null;
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            }

            primaryStage.setTitle("Gestion des Naissances — Connexion Sécurisée");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("[MainApp] Portail de connexion prêt et affiché.");
        } catch (IOException e) {
            System.err.println("[MainApp] Erreur lors du chargement de la vue de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
