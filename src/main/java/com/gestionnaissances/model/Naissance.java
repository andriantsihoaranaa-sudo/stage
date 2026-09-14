package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Modèle de données représentant un Acte / Déclaration de Naissance.
 * Utilise les propriétés JavaFX (Property) pour un affichage réactif dans les tables et formulaires.
 */
public class Naissance {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty numeroActe = new SimpleStringProperty();
    private final IntegerProperty anneeRegistre = new SimpleIntegerProperty();
    private final StringProperty dateEnregistrement = new SimpleStringProperty();
    
    // Informations de l'enfant
    private final StringProperty nomEnfant = new SimpleStringProperty();
    private final StringProperty prenomsEnfant = new SimpleStringProperty();
    private final StringProperty sexe = new SimpleStringProperty();
    private final StringProperty dateNaissance = new SimpleStringProperty();
    private final StringProperty heureNaissance = new SimpleStringProperty();
    private final StringProperty lieuNaissance = new SimpleStringProperty();

    // Informations des parents
    private final StringProperty nomPere = new SimpleStringProperty();
    private final StringProperty prenomPere = new SimpleStringProperty();
    private final StringProperty professionPere = new SimpleStringProperty();
    private final StringProperty domicilePere = new SimpleStringProperty();

    private final StringProperty nomMere = new SimpleStringProperty();
    private final StringProperty prenomMere = new SimpleStringProperty();
    private final StringProperty professionMere = new SimpleStringProperty();
    private final StringProperty domicileMere = new SimpleStringProperty();

    // Déclarant & Officier
    private final StringProperty nomDeclarant = new SimpleStringProperty();
    private final StringProperty qualiteDeclarant = new SimpleStringProperty();
    private final StringProperty officierEtatCivil = new SimpleStringProperty();
    private final StringProperty observations = new SimpleStringProperty();

    public Naissance() {
    }

    public Naissance(String numeroActe, int anneeRegistre, String dateEnregistrement,
                     String nomEnfant, String prenomsEnfant, String sexe, String dateNaissance,
                     String heureNaissance, String lieuNaissance,
                     String nomPere, String prenomPere, String professionPere, String domicilePere,
                     String nomMere, String prenomMere, String professionMere, String domicileMere,
                     String nomDeclarant, String qualiteDeclarant, String officierEtatCivil,
                     String observations) {
        setNumeroActe(numeroActe);
        setAnneeRegistre(anneeRegistre);
        setDateEnregistrement(dateEnregistrement);
        setNomEnfant(nomEnfant);
        setPrenomsEnfant(prenomsEnfant);
        setSexe(sexe);
        setDateNaissance(dateNaissance);
        setHeureNaissance(heureNaissance);
        setLieuNaissance(lieuNaissance);
        setNomPere(nomPere);
        setPrenomPere(prenomPere);
        setProfessionPere(professionPere);
        setDomicilePere(domicilePere);
        setNomMere(nomMere);
        setPrenomMere(prenomMere);
        setProfessionMere(professionMere);
        setDomicileMere(domicileMere);
        setNomDeclarant(nomDeclarant);
        setQualiteDeclarant(qualiteDeclarant);
        setOfficierEtatCivil(officierEtatCivil);
        setObservations(observations);
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public String getNumeroActe() { return numeroActe.get(); }
    public void setNumeroActe(String value) { numeroActe.set(value); }
    public StringProperty numeroActeProperty() { return numeroActe; }

    public int getAnneeRegistre() { return anneeRegistre.get(); }
    public void setAnneeRegistre(int value) { anneeRegistre.set(value); }
    public IntegerProperty anneeRegistreProperty() { return anneeRegistre; }

    public String getDateEnregistrement() { return dateEnregistrement.get(); }
    public void setDateEnregistrement(String value) { dateEnregistrement.set(value); }
    public StringProperty dateEnregistrementProperty() { return dateEnregistrement; }

    public String getNomEnfant() { return nomEnfant.get(); }
    public void setNomEnfant(String value) { nomEnfant.set(value); }
    public StringProperty nomEnfantProperty() { return nomEnfant; }

    public String getPrenomsEnfant() { return prenomsEnfant.get(); }
    public void setPrenomsEnfant(String value) { prenomsEnfant.set(value); }
    public StringProperty prenomsEnfantProperty() { return prenomsEnfant; }

    public String getNomCompletEnfant() {
        return (getNomEnfant() != null ? getNomEnfant() : "") + " " +
               (getPrenomsEnfant() != null ? getPrenomsEnfant() : "");
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

    public String getNomPere() { return nomPere.get(); }
    public void setNomPere(String value) { nomPere.set(value); }
    public StringProperty nomPereProperty() { return nomPere; }

    public String getPrenomPere() { return prenomPere.get(); }
    public void setPrenomPere(String value) { prenomPere.set(value); }
    public StringProperty prenomPereProperty() { return prenomPere; }

    public String getProfessionPere() { return professionPere.get(); }
    public void setProfessionPere(String value) { professionPere.set(value); }
    public StringProperty professionPereProperty() { return professionPere; }

    public String getDomicilePere() { return domicilePere.get(); }
    public void setDomicilePere(String value) { domicilePere.set(value); }
    public StringProperty domicilePereProperty() { return domicilePere; }

    public String getNomMere() { return nomMere.get(); }
    public void setNomMere(String value) { nomMere.set(value); }
    public StringProperty nomMereProperty() { return nomMere; }

    public String getPrenomMere() { return prenomMere.get(); }
    public void setPrenomMere(String value) { prenomMere.set(value); }
    public StringProperty prenomMereProperty() { return prenomMere; }

    public String getProfessionMere() { return professionMere.get(); }
    public void setProfessionMere(String value) { professionMere.set(value); }
    public StringProperty professionMereProperty() { return professionMere; }

    public String getDomicileMere() { return domicileMere.get(); }
    public void setDomicileMere(String value) { domicileMere.set(value); }
    public StringProperty domicileMereProperty() { return domicileMere; }

    public String getNomDeclarant() { return nomDeclarant.get(); }
    public void setNomDeclarant(String value) { nomDeclarant.set(value); }
    public StringProperty nomDeclarantProperty() { return nomDeclarant; }

    public String getQualiteDeclarant() { return qualiteDeclarant.get(); }
    public void setQualiteDeclarant(String value) { qualiteDeclarant.set(value); }
    public StringProperty qualiteDeclarantProperty() { return qualiteDeclarant; }

    public String getOfficierEtatCivil() { return officierEtatCivil.get(); }
    public void setOfficierEtatCivil(String value) { officierEtatCivil.set(value); }
    public StringProperty officierEtatCivilProperty() { return officierEtatCivil; }

    public String getObservations() { return observations.get(); }
    public void setObservations(String value) { observations.set(value); }
    public StringProperty observationsProperty() { return observations; }
}
