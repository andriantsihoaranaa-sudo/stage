package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Entité d'audit représentant l'historique d'une modification apportée à un dossier de naissance.
 * Correspond à la table SQLite 'historique'.
 * Garantit la traçabilité intégrale de chaque altération de donnée.
 */
public class HistoriqueModification {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty enfantId = new SimpleIntegerProperty();
    private final IntegerProperty utilisateurId = new SimpleIntegerProperty();
    private final StringProperty dateModification = new SimpleStringProperty();
    private final StringProperty heureModification = new SimpleStringProperty();
    private final StringProperty informationModifiee = new SimpleStringProperty();
    private final StringProperty ancienneValeur = new SimpleStringProperty();
    private final StringProperty nouvelleValeur = new SimpleStringProperty();

    public HistoriqueModification() {
    }

    public HistoriqueModification(int enfantId, Integer utilisateurId, String dateModification,
                                  String heureModification, String informationModifiee,
                                  String ancienneValeur, String nouvelleValeur) {
        setEnfantId(enfantId);
        if (utilisateurId != null) {
            setUtilisateurId(utilisateurId);
        }
        setDateModification(dateModification);
        setHeureModification(heureModification);
        setInformationModifiee(informationModifiee);
        setAncienneValeur(ancienneValeur);
        setNouvelleValeur(nouvelleValeur);
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getEnfantId() { return enfantId.get(); }
    public void setEnfantId(int value) { enfantId.set(value); }
    public IntegerProperty enfantIdProperty() { return enfantId; }

    public int getUtilisateurId() { return utilisateurId.get(); }
    public void setUtilisateurId(int value) { utilisateurId.set(value); }
    public IntegerProperty utilisateurIdProperty() { return utilisateurId; }

    public String getDateModification() { return dateModification.get(); }
    public void setDateModification(String value) { dateModification.set(value); }
    public StringProperty dateModificationProperty() { return dateModification; }

    public String getHeureModification() { return heureModification.get(); }
    public void setHeureModification(String value) { heureModification.set(value); }
    public StringProperty heureModificationProperty() { return heureModification; }

    public String getInformationModifiee() { return informationModifiee.get(); }
    public void setInformationModifiee(String value) { informationModifiee.set(value); }
    public StringProperty informationModifieeProperty() { return informationModifiee; }

    public String getAncienneValeur() { return ancienneValeur.get(); }
    public void setAncienneValeur(String value) { ancienneValeur.set(value); }
    public StringProperty ancienneValeurProperty() { return ancienneValeur; }

    public String getNouvelleValeur() { return nouvelleValeur.get(); }
    public void setNouvelleValeur(String value) { nouvelleValeur.set(value); }
    public StringProperty nouvelleValeurProperty() { return nouvelleValeur; }
}
