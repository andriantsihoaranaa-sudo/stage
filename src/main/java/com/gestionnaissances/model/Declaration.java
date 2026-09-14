package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Entité représentant la déclaration de naissance officielle.
 * Correspond à la table SQLite 'declarations'.
 */
public class Declaration {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty enfantId = new SimpleIntegerProperty();
    private final StringProperty numeroDeclaration = new SimpleStringProperty();
    private final IntegerProperty anneeDeclaration = new SimpleIntegerProperty();
    private final StringProperty dateDeclaration = new SimpleStringProperty();
    private final StringProperty heureDeclaration = new SimpleStringProperty();
    private final StringProperty nomDeclarant = new SimpleStringProperty();
    private final StringProperty qualiteDeclarant = new SimpleStringProperty();
    private final StringProperty observations = new SimpleStringProperty();

    public Declaration() {
    }

    public Declaration(int enfantId, String numeroDeclaration, int anneeDeclaration,
                       String dateDeclaration, String heureDeclaration, String nomDeclarant,
                       String qualiteDeclarant, String observations) {
        setEnfantId(enfantId);
        setNumeroDeclaration(numeroDeclaration);
        setAnneeDeclaration(anneeDeclaration);
        setDateDeclaration(dateDeclaration);
        setHeureDeclaration(heureDeclaration);
        setNomDeclarant(nomDeclarant);
        setQualiteDeclarant(qualiteDeclarant);
        setObservations(observations);
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getEnfantId() { return enfantId.get(); }
    public void setEnfantId(int value) { enfantId.set(value); }
    public IntegerProperty enfantIdProperty() { return enfantId; }

    public String getNumeroDeclaration() { return numeroDeclaration.get(); }
    public void setNumeroDeclaration(String value) { numeroDeclaration.set(value); }
    public StringProperty numeroDeclarationProperty() { return numeroDeclaration; }

    public int getAnneeDeclaration() { return anneeDeclaration.get(); }
    public void setAnneeDeclaration(int value) { anneeDeclaration.set(value); }
    public IntegerProperty anneeDeclarationProperty() { return anneeDeclaration; }

    public String getDateDeclaration() { return dateDeclaration.get(); }
    public void setDateDeclaration(String value) { dateDeclaration.set(value); }
    public StringProperty dateDeclarationProperty() { return dateDeclaration; }

    public String getHeureDeclaration() { return heureDeclaration.get(); }
    public void setHeureDeclaration(String value) { heureDeclaration.set(value); }
    public StringProperty heureDeclarationProperty() { return heureDeclaration; }

    public String getNomDeclarant() { return nomDeclarant.get(); }
    public void setNomDeclarant(String value) { nomDeclarant.set(value); }
    public StringProperty nomDeclarantProperty() { return nomDeclarant; }

    public String getQualiteDeclarant() { return qualiteDeclarant.get(); }
    public void setQualiteDeclarant(String value) { qualiteDeclarant.set(value); }
    public StringProperty qualiteDeclarantProperty() { return qualiteDeclarant; }

    public String getObservations() { return observations.get(); }
    public void setObservations(String value) { observations.set(value); }
    public StringProperty observationsProperty() { return observations; }
}
