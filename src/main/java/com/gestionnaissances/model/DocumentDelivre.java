package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Entité représentant les documents officiels délivrés pour un dossier de naissance.
 * Correspond à la table SQLite 'documents'.
 */
public class DocumentDelivre {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty enfantId = new SimpleIntegerProperty();
    private final StringProperty typeDocument = new SimpleStringProperty();
    private final StringProperty numeroDocument = new SimpleStringProperty();
    private final StringProperty dateDelivrance = new SimpleStringProperty();
    private final StringProperty heureDelivrance = new SimpleStringProperty();
    private final StringProperty nomPersonneReception = new SimpleStringProperty();
    private final StringProperty qualitePersonneReception = new SimpleStringProperty();
    private final BooleanProperty confirmationLivraison = new SimpleBooleanProperty(false);

    public DocumentDelivre() {
    }

    public DocumentDelivre(int enfantId, String typeDocument, String numeroDocument,
                           String dateDelivrance, String heureDelivrance,
                           String nomPersonneReception, String qualitePersonneReception,
                           boolean confirmationLivraison) {
        setEnfantId(enfantId);
        setTypeDocument(typeDocument);
        setNumeroDocument(numeroDocument);
        setDateDelivrance(dateDelivrance);
        setHeureDelivrance(heureDelivrance);
        setNomPersonneReception(nomPersonneReception);
        setQualitePersonneReception(qualitePersonneReception);
        setConfirmationLivraison(confirmationLivraison);
    }

    public DocumentDelivre(int enfantId, String typeDocument, String numeroDocument,
                           String dateDelivrance, String nomPersonneReception,
                           String qualitePersonneReception, boolean confirmationLivraison) {
        this(enfantId, typeDocument, numeroDocument, dateDelivrance, "", nomPersonneReception, qualitePersonneReception, confirmationLivraison);
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getEnfantId() { return enfantId.get(); }
    public void setEnfantId(int value) { enfantId.set(value); }
    public IntegerProperty enfantIdProperty() { return enfantId; }

    public String getTypeDocument() { return typeDocument.get(); }
    public void setTypeDocument(String value) { typeDocument.set(value); }
    public StringProperty typeDocumentProperty() { return typeDocument; }

    public String getNumeroDocument() { return numeroDocument.get(); }
    public void setNumeroDocument(String value) { numeroDocument.set(value); }
    public StringProperty numeroDocumentProperty() { return numeroDocument; }

    public String getDateDelivrance() { return dateDelivrance.get(); }
    public void setDateDelivrance(String value) { dateDelivrance.set(value); }
    public StringProperty dateDelivranceProperty() { return dateDelivrance; }

    public String getHeureDelivrance() { return heureDelivrance.get(); }
    public void setHeureDelivrance(String value) { heureDelivrance.set(value); }
    public StringProperty heureDelivranceProperty() { return heureDelivrance; }

    public String getNomPersonneReception() { return nomPersonneReception.get(); }
    public void setNomPersonneReception(String value) { nomPersonneReception.set(value); }
    public StringProperty nomPersonneReceptionProperty() { return nomPersonneReception; }

    public String getQualitePersonneReception() { return qualitePersonneReception.get(); }
    public void setQualitePersonneReception(String value) { qualitePersonneReception.set(value); }
    public StringProperty qualitePersonneReceptionProperty() { return qualitePersonneReception; }

    public boolean isConfirmationLivraison() { return confirmationLivraison.get(); }
    public void setConfirmationLivraison(boolean value) { confirmationLivraison.set(value); }
    public BooleanProperty confirmationLivraisonProperty() { return confirmationLivraison; }
}
