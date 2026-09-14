package com.gestionnaissances.model;

import javafx.beans.property.*;

/**
 * Modèle composite représentant le dossier complet d'une déclaration de naissance.
 * Regroupe les entités relationnelles : Enfant, Parents, Déclaration, Enregistrement communal et Document officiel.
 */
public class DossierNaissance {

    private Enfant enfant;
    private ParentInfo parents;
    private Declaration declaration;
    private EnregistrementCommune commune;
    private DocumentDelivre document;

    // Propriétés calculées pour affichage direct dans les TableView
    private final StringProperty numeroRegistre = new SimpleStringProperty();
    private final StringProperty identifiantUnique = new SimpleStringProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty prenoms = new SimpleStringProperty();
    private final StringProperty sexe = new SimpleStringProperty();
    private final StringProperty dateNaissance = new SimpleStringProperty();
    private final StringProperty lieuNaissance = new SimpleStringProperty();
    private final StringProperty anneeDeclaration = new SimpleStringProperty();
    private final StringProperty statut = new SimpleStringProperty();
    private final StringProperty nomPere = new SimpleStringProperty();
    private final StringProperty nomMere = new SimpleStringProperty();

    public DossierNaissance() {
        this.enfant = new Enfant();
        this.parents = new ParentInfo();
        this.declaration = new Declaration();
        this.commune = new EnregistrementCommune();
        this.document = new DocumentDelivre();
    }

    public DossierNaissance(Enfant enfant, ParentInfo parents, Declaration declaration,
                            EnregistrementCommune commune, DocumentDelivre document) {
        setEnfant(enfant);
        setParents(parents);
        setDeclaration(declaration);
        setCommune(commune);
        setDocument(document);
        synchroniserProprietes();
    }

    public void synchroniserProprietes() {
        if (enfant != null) {
            numeroRegistre.set(enfant.getNumeroRegistre());
            identifiantUnique.set(enfant.getIdentifiantUnique());
            nom.set(enfant.getNom());
            prenoms.set(enfant.getPrenoms());
            sexe.set(enfant.getSexe());
            dateNaissance.set(enfant.getDateNaissance());
            lieuNaissance.set(enfant.getLieuNaissance());
            statut.set(enfant.getStatut());
        }
        if (declaration != null && declaration.getAnneeDeclaration() > 0) {
            anneeDeclaration.set(String.valueOf(declaration.getAnneeDeclaration()));
        } else if (enfant != null && enfant.getDateNaissance() != null && enfant.getDateNaissance().length() >= 4) {
            anneeDeclaration.set(enfant.getDateNaissance().substring(0, 4));
        } else {
            anneeDeclaration.set("-");
        }
        if (parents != null) {
            nomPere.set((parents.getPrenomPere() != null ? parents.getPrenomPere() + " " : "") + (parents.getNomPere() != null ? parents.getNomPere() : "-"));
            nomMere.set((parents.getPrenomMere() != null ? parents.getPrenomMere() + " " : "") + (parents.getNomMere() != null ? parents.getNomMere() : "-"));
        }
    }

    public Enfant getEnfant() { return enfant; }
    public void setEnfant(Enfant enfant) {
        this.enfant = enfant;
        synchroniserProprietes();
    }

    public ParentInfo getParents() { return parents; }
    public void setParents(ParentInfo parents) {
        this.parents = parents;
        synchroniserProprietes();
    }

    public Declaration getDeclaration() { return declaration; }
    public void setDeclaration(Declaration declaration) {
        this.declaration = declaration;
        synchroniserProprietes();
    }

    public EnregistrementCommune getCommune() { return commune; }
    public void setCommune(EnregistrementCommune commune) {
        this.commune = commune;
    }
    public EnregistrementCommune getEnregistrementCommune() { return commune; }
    public void setEnregistrementCommune(EnregistrementCommune commune) {
        this.commune = commune;
    }

    public DocumentDelivre getDocument() { return document; }
    public void setDocument(DocumentDelivre document) {
        this.document = document;
    }
    public DocumentDelivre getDocumentDelivre() { return document; }
    public void setDocumentDelivre(DocumentDelivre document) {
        this.document = document;
    }

    // Propriétés pour TableView
    public int getId() { return enfant != null ? enfant.getId() : 0; }
    public void setId(int id) {
        if (enfant != null) {
            enfant.setId(id);
        }
    }
    public String getNumeroRegistre() { return numeroRegistre.get(); }
    public StringProperty numeroRegistreProperty() { return numeroRegistre; }

    public String getIdentifiantUnique() { return identifiantUnique.get(); }
    public StringProperty identifiantUniqueProperty() { return identifiantUnique; }

    public String getNom() { return nom.get(); }
    public StringProperty nomProperty() { return nom; }

    public String getPrenoms() { return prenoms.get(); }
    public StringProperty prenomsProperty() { return prenoms; }

    public String getSexe() { return sexe.get(); }
    public StringProperty sexeProperty() { return sexe; }

    public String getDateNaissance() { return dateNaissance.get(); }
    public StringProperty dateNaissanceProperty() { return dateNaissance; }

    public String getLieuNaissance() { return lieuNaissance.get(); }
    public StringProperty lieuNaissanceProperty() { return lieuNaissance; }

    public String getAnneeDeclaration() { return anneeDeclaration.get(); }
    public StringProperty anneeDeclarationProperty() { return anneeDeclaration; }

    public String getStatut() { return statut.get(); }
    public StringProperty statutProperty() { return statut; }

    public String getNomPere() { return nomPere.get(); }
    public StringProperty nomPereProperty() { return nomPere; }

    public String getNomMere() { return nomMere.get(); }
    public StringProperty nomMereProperty() { return nomMere; }
}
