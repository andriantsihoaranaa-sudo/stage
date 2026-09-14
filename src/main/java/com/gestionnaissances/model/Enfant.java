package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Entité représentant un Enfant dans le registre d'état civil.
 * Correspond à la table SQLite 'enfants'.
 */
public class Enfant {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty identifiantUnique = new SimpleStringProperty();
    private final StringProperty numeroRegistre = new SimpleStringProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty prenoms = new SimpleStringProperty();
    private final StringProperty sexe = new SimpleStringProperty();
    private final StringProperty dateNaissance = new SimpleStringProperty();
    private final StringProperty heureNaissance = new SimpleStringProperty();
    private final StringProperty lieuNaissance = new SimpleStringProperty();
    private final StringProperty statut = new SimpleStringProperty("Enfant");
    private final StringProperty dateCreation = new SimpleStringProperty();

    public Enfant() {
    }

    public Enfant(String identifiantUnique, String numeroRegistre, String nom, String prenoms,
                  String sexe, String dateNaissance, String heureNaissance, String lieuNaissance,
                  String statut) {
        setIdentifiantUnique(identifiantUnique);
        setNumeroRegistre(numeroRegistre);
        setNom(nom);
        setPrenoms(prenoms);
        setSexe(sexe);
        setDateNaissance(dateNaissance);
        setHeureNaissance(heureNaissance);
        setLieuNaissance(lieuNaissance);
        setStatut(statut != null ? statut : "Enfant");
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public String getIdentifiantUnique() { return identifiantUnique.get(); }
    public void setIdentifiantUnique(String value) { identifiantUnique.set(value); }
    public StringProperty identifiantUniqueProperty() { return identifiantUnique; }

    public String getNumeroRegistre() { return numeroRegistre.get(); }
    public void setNumeroRegistre(String value) { numeroRegistre.set(value); }
    public StringProperty numeroRegistreProperty() { return numeroRegistre; }

    public String getNom() { return nom.get(); }
    public void setNom(String value) { nom.set(value); }
    public StringProperty nomProperty() { return nom; }

    public String getPrenoms() { return prenoms.get(); }
    public void setPrenoms(String value) { prenoms.set(value); }
    public StringProperty prenomsProperty() { return prenoms; }

    public String getNomComplet() {
        return (getNom() != null ? getNom() : "") + " " + (getPrenoms() != null ? getPrenoms() : "");
    }

    public String getSexe() { return sexe.get(); }
    public void setSexe(String value) { sexe.set(value); }
    public StringProperty sexeProperty() { return sexe; }

    public String getDateNaissance() { return dateNaissance.get(); }
    public void setDateNaissance(String value) { dateNaissance.set(value); }
    public StringProperty dateNaissanceProperty() { return dateNaissance; }

    public String getHeureNaissance() { return heureNaissance.get(); }
    public void setHeureNaissance(String value) { heureNaissance.set(value); }
    public StringProperty heureNaissanceProperty() { return heureNaissance; }

    public String getLieuNaissance() { return lieuNaissance.get(); }
    public void setLieuNaissance(String value) { lieuNaissance.set(value); }
    public StringProperty lieuNaissanceProperty() { return lieuNaissance; }

    public String getStatut() { return statut.get(); }
    public void setStatut(String value) { statut.set(value); }
    public StringProperty statutProperty() { return statut; }

    public String getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(String value) { dateCreation.set(value); }
    public StringProperty dateCreationProperty() { return dateCreation; }
}
