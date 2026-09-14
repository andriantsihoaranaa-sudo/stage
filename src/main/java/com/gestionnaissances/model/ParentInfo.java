package com.gestionnaissances.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Entité représentant les informations des parents de l'enfant.
 * Correspond à la table SQLite 'parents'.
 * Inclut le calcul dynamique et automatique de l'âge à partir des dates de naissance des parents.
 */
public class ParentInfo {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty enfantId = new SimpleIntegerProperty();

    // Père
    private final StringProperty nomPere = new SimpleStringProperty();
    private final StringProperty prenomPere = new SimpleStringProperty();
    private final StringProperty dateNaissancePere = new SimpleStringProperty();
    private final IntegerProperty agePere = new SimpleIntegerProperty();
    private final StringProperty professionPere = new SimpleStringProperty();
    private final StringProperty domicilePere = new SimpleStringProperty();

    // Mère
    private final StringProperty nomMere = new SimpleStringProperty();
    private final StringProperty prenomMere = new SimpleStringProperty();
    private final StringProperty dateNaissanceMere = new SimpleStringProperty();
    private final IntegerProperty ageMere = new SimpleIntegerProperty();
    private final StringProperty professionMere = new SimpleStringProperty();
    private final StringProperty domicileMere = new SimpleStringProperty();

    public ParentInfo() {
    }

    public ParentInfo(int enfantId, String nomPere, String prenomPere, String dateNaissancePere,
                      Integer agePere, String professionPere, String domicilePere,
                      String nomMere, String prenomMere, String dateNaissanceMere,
                      Integer ageMere, String professionMere, String domicileMere) {
        setEnfantId(enfantId);
        setNomPere(nomPere);
        setPrenomPere(prenomPere);
        setDateNaissancePere(dateNaissancePere);
        if (agePere != null && agePere > 0) {
            setAgePere(agePere);
        } else if (dateNaissancePere != null && !dateNaissancePere.isBlank()) {
            setAgePere(calculerAge(dateNaissancePere));
        }
        setProfessionPere(professionPere);
        setDomicilePere(domicilePere);

        setNomMere(nomMere);
        setPrenomMere(prenomMere);
        setDateNaissanceMere(dateNaissanceMere);
        if (ageMere != null && ageMere > 0) {
            setAgeMere(ageMere);
        } else if (dateNaissanceMere != null && !dateNaissanceMere.isBlank()) {
            setAgeMere(calculerAge(dateNaissanceMere));
        }
        setProfessionMere(professionMere);
        setDomicileMere(domicileMere);
    }

    public ParentInfo(int enfantId, String nomPere, String prenomPere, String dateNaissancePere,
                      Integer agePere, String professionPere, String nomMere, String prenomMere,
                      String dateNaissanceMere, Integer ageMere, String professionMere) {
        this(enfantId, nomPere, prenomPere, dateNaissancePere, agePere, professionPere, "",
             nomMere, prenomMere, dateNaissanceMere, ageMere, professionMere, "");
    }

    /**
     * Calcule automatiquement l'âge à partir d'une date de naissance (format standard ISO YYYY-MM-DD).
     */
    public static int calculerAge(String dateNaissanceStr) {
        if (dateNaissanceStr == null || dateNaissanceStr.trim().isEmpty()) {
            return 0;
        }
        try {
            LocalDate dateNaiss = LocalDate.parse(dateNaissanceStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return Period.between(dateNaiss, LocalDate.now()).getYears();
        } catch (Exception e) {
            return 0;
        }
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getEnfantId() { return enfantId.get(); }
    public void setEnfantId(int value) { enfantId.set(value); }
    public IntegerProperty enfantIdProperty() { return enfantId; }

    public String getNomPere() { return nomPere.get(); }
    public void setNomPere(String value) { nomPere.set(value); }
    public StringProperty nomPereProperty() { return nomPere; }

    public String getPrenomPere() { return prenomPere.get(); }
    public void setPrenomPere(String value) { prenomPere.set(value); }
    public StringProperty prenomPereProperty() { return prenomPere; }

    public String getDateNaissancePere() { return dateNaissancePere.get(); }
    public void setDateNaissancePere(String value) {
        dateNaissancePere.set(value);
        if (value != null && !value.isBlank() && getAgePere() <= 0) {
            setAgePere(calculerAge(value));
        }
    }
    public StringProperty dateNaissancePereProperty() { return dateNaissancePere; }

    public int getAgePere() {
        if (dateNaissancePere.get() != null && !dateNaissancePere.get().isBlank()) {
            return calculerAge(dateNaissancePere.get());
        }
        return agePere.get();
    }
    public void setAgePere(int value) { agePere.set(value); }
    public IntegerProperty agePereProperty() { return agePere; }

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

    public String getDateNaissanceMere() { return dateNaissanceMere.get(); }
    public void setDateNaissanceMere(String value) {
        dateNaissanceMere.set(value);
        if (value != null && !value.isBlank() && getAgeMere() <= 0) {
            setAgeMere(calculerAge(value));
        }
    }
    public StringProperty dateNaissanceMereProperty() { return dateNaissanceMere; }

    public int getAgeMere() {
        if (dateNaissanceMere.get() != null && !dateNaissanceMere.get().isBlank()) {
            return calculerAge(dateNaissanceMere.get());
        }
        return ageMere.get();
    }
    public void setAgeMere(int value) { ageMere.set(value); }
    public IntegerProperty ageMereProperty() { return ageMere; }

    public String getProfessionMere() { return professionMere.get(); }
    public void setProfessionMere(String value) { professionMere.set(value); }
    public StringProperty professionMereProperty() { return professionMere; }

    public String getDomicileMere() { return domicileMere.get(); }
    public void setDomicileMere(String value) { domicileMere.set(value); }
    public StringProperty domicileMereProperty() { return domicileMere; }
}
