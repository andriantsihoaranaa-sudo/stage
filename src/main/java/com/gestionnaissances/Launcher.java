package com.gestionnaissances;

/**
 * Point d'entrée principal sans héritage d'Application.
 * Cette classe permet de lancer l'application JavaFX sous forme d'exécutable JAR
 * autonome (Fat-JAR) sous Windows ou Linux sans configuration de module externe.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
