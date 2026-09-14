package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Entité représentant l'enregistrement officiel validé par la commune ou le centre d'état civil.
 * Correspond à la table SQLite 'enregistrements_commune'.
 */
public class EnregistrementCommune {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty enfantId = new SimpleIntegerProperty();
    private final StringProperty nomResponsable = new SimpleStringProperty();
    private final StringProperty fonctionResponsable = new SimpleStringProperty();
    private final StringProperty dateEnregistrement = new SimpleStringProperty();
    private final StringProperty heureEnregistrement = new SimpleStringProperty();
    private final StringProperty folio = new SimpleStringProperty();
    private final StringProperty tome = new SimpleStringProperty();
    private final StringProperty statutValidation = new SimpleStringProperty("VALIDE");

    public EnregistrementCommune() {
    }

    public EnregistrementCommune(int enfantId, String nomResponsable, String fonctionResponsable,
                                  String dateEnregistrement, String heureEnregistrement,
                                  String folio, String tome, String statutValidation) {
        setEnfantId(enfantId);
        setNomResponsable(nomResponsable);
        setFonctionResponsable(fonctionResponsable);
        setDateEnregistrement(dateEnregistrement);
        setHeureEnregistrement(heureEnregistrement);
        setFolio(folio);
        setTome(tome);
        setStatutValidation(statutValidation != null ? statutValidation : "VALIDE");
    }

    public EnregistrementCommune(int enfantId, String nomResponsable, String fonctionResponsable,
                                  String dateEnregistrement, String heureEnregistrement) {
        this(enfantId, nomResponsable, fonctionResponsable, dateEnregistrement, heureEnregistrement, "", "", "VALIDE");
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getEnfantId() { return enfantId.get(); }
    public void setEnfantId(int value) { enfantId.set(value); }
    public IntegerProperty enfantIdProperty() { return enfantId; }

    public String getNomResponsable() { return nomResponsable.get(); }
    public void setNomResponsable(String value) { nomResponsable.set(value); }
    public StringProperty nomResponsableProperty() { return nomResponsable; }

    public String getFonctionResponsable() { return fonctionResponsable.get(); }
    public void setFonctionResponsable(String value) { fonctionResponsable.set(value); }
    public StringProperty fonctionResponsableProperty() { return fonctionResponsable; }

    public String getDateEnregistrement() { return dateEnregistrement.get(); }
    public void setDateEnregistrement(String value) { dateEnregistrement.set(value); }
    public StringProperty dateEnregistrementProperty() { return dateEnregistrement; }

    public String getHeureEnregistrement() { return heureEnregistrement.get(); }
    public void setHeureEnregistrement(String value) { heureEnregistrement.set(value); }
    public StringProperty heureEnregistrementProperty() { return heureEnregistrement; }

    public String getFolio() { return folio.get(); }
    public void setFolio(String value) { folio.set(value); }
    public StringProperty folioProperty() { return folio; }

    public String getTome() { return tome.get(); }
    public void setTome(String value) { tome.set(value); }
    public StringProperty tomeProperty() { return tome; }

    public String getStatutValidation() { return statutValidation.get(); }
    public void setStatutValidation(String value) { statutValidation.set(value); }
    public StringProperty statutValidationProperty() { return statutValidation; }
}
