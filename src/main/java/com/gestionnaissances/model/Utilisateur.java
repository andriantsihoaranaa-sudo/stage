package com.gestionnaissances.model;

import com.gestionnaissances.security.Role;
import javafx.beans.property.*;

/**
 * Entité représentant un utilisateur du système de gestion des naissances.
 * Correspond à la table SQLite 'utilisateurs'.
 * Rôles officiels : ADMINISTRATEUR, RESPONSABLE_COMMUNAL, AGENT.
 */
public class Utilisateur {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nomUtilisateur = new SimpleStringProperty();
    private final StringProperty motDePasseHash = new SimpleStringProperty();
    private final StringProperty nomComplet = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty("AGENT");
    private final BooleanProperty actif = new SimpleBooleanProperty(true);
    private final StringProperty dateCreation = new SimpleStringProperty();
    private final StringProperty derniereConnexion = new SimpleStringProperty();

    public Utilisateur() {
    }

    public Utilisateur(String nomUtilisateur, String motDePasseHash, String role, boolean actif) {
        setNomUtilisateur(nomUtilisateur);
        setMotDePasseHash(motDePasseHash);
        setRole(role != null ? role : "AGENT");
        setActif(actif);
    }

    public Utilisateur(String nomUtilisateur, String motDePasseHash, String nomComplet, String role, boolean actif) {
        setNomUtilisateur(nomUtilisateur);
        setMotDePasseHash(motDePasseHash);
        setNomComplet(nomComplet);
        setRole(role != null ? role : "AGENT");
        setActif(actif);
    }

    // Getters / Setters / Properties
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public String getNomUtilisateur() { return nomUtilisateur.get(); }
    public void setNomUtilisateur(String value) { nomUtilisateur.set(value != null ? value.trim().toLowerCase() : ""); }
    public StringProperty nomUtilisateurProperty() { return nomUtilisateur; }

    public String getMotDePasseHash() { return motDePasseHash.get(); }
    public void setMotDePasseHash(String value) { motDePasseHash.set(value); }
    public StringProperty motDePasseHashProperty() { return motDePasseHash; }

    // Rétrocompatibilité : getMotDePasse retourne le hash stocké
    public String getMotDePasse() { return getMotDePasseHash(); }
    public void setMotDePasse(String value) { setMotDePasseHash(value); }

    public String getNomComplet() { return nomComplet.get(); }
    public void setNomComplet(String value) { nomComplet.set(value); }
    public StringProperty nomCompletProperty() { return nomComplet; }

    public String getRole() { return role.get(); }
    public void setRole(String value) { role.set(value != null ? value : "AGENT"); }
    public StringProperty roleProperty() { return role; }

    public Role getRoleEnum() {
        return Role.depuisChaine(getRole());
    }

    public boolean isActif() { return actif.get(); }
    public void setActif(boolean value) { actif.set(value); }
    public BooleanProperty actifProperty() { return actif; }

    public String getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(String value) { dateCreation.set(value); }
    public StringProperty dateCreationProperty() { return dateCreation; }

    public String getDerniereConnexion() { return derniereConnexion.get(); }
    public void setDerniereConnexion(String value) { derniereConnexion.set(value); }
    public StringProperty derniereConnexionProperty() { return derniereConnexion; }
}
