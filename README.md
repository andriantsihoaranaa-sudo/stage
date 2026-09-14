# 🏛️ Gestion des Naissances — Application Desktop Hors-Ligne

Application de bureau native pour la saisie, l'enregistrement, la consultation et la conservation numérique pérenne des déclarations et actes de naissance pour les mairies, communes et centres d'état civil.

Ce logiciel fonctionne **entièrement hors-ligne** (sans connexion Internet et sans navigateur web requis).

---

## 🛠️ Technologies Utilisées

* **Langage** : Java 17 LTS
* **Interface Graphique** : JavaFX (Contrôles FXML & CSS personnalisé)
* **Base de Données Locale** : SQLite (`naissances.db`)
* **Accès aux Données** : JDBC via le pilote `sqlite-jdbc` (requêtes préparées)
* **Gestionnaire de Projet** : Apache Maven

---

## 📂 Structure du Projet Maven

```text
gestion-naissances/
├── pom.xml                                    # Configuration Maven, dépendances et plugins
├── README.md                                  # Documentation complète
├── run.sh                                     # Script de lancement rapide Linux / macOS
├── run.bat                                    # Script de lancement rapide Windows
└── src/
    ├── main/
    │   ├── java/com/gestionnaissances/
    │   │   ├── Launcher.java                  # Point d'entrée autonome pour JAR exécutable
    │   │   ├── MainApp.java                   # Application JavaFX & initialisation SQLite
    │   │   ├── controller/
    │   │   │   └── MainController.java        # Contrôleur FXML (CRUD, validation, filtres)
    │   │   ├── dao/
    │   │   │   └── NaissanceDAO.java          # Opérations JDBC sécurisées (PreparedStatement)
    │   │   ├── database/
    │   │   │   └── DatabaseManager.java       # Connexion SQLite locale & création des tables
    │   │   └── model/
    │   │       └── Naissance.java             # Modèle de données JavaFX Property
    │   └── resources/com/gestionnaissances/
    │       ├── view/
    │       │   └── main-view.fxml             # Interface graphique déclarative FXML
    │       └── css/
    │           └── style.css                  # Thème visuel professionnel officiel
    └── test/
        └── java/com/gestionnaissances/
            └── database/
                └── DatabaseManagerTest.java   # Tests unitaires JUnit 5
```

---

## 🚀 Démarrage Rapide

### Prérequis
* **Java JDK 17** ou supérieur installé
* **Apache Maven** installé

### 1. Compiler le projet
```bash
mvn clean compile
```

### 2. Exécuter les tests unitaires
```bash
mvn test
```

### 3. Lancer l'application en mode développement
```bash
mvn javafx:run
```

### 4. Générer le fichier exécutable autonome (.JAR)
Le plugin `maven-shade-plugin` est configuré pour produire un fichier JAR contenant toutes les dépendances (JavaFX + SQLite JDBC) :
```bash
mvn clean package
```
Le fichier JAR généré se trouvera dans :
```text
target/gestion-naissances-1.0.0.jar
```

Sous **Windows**, l'utilisateur peut alors simplement double-cliquer sur le fichier `.jar` ou lancer :
```cmd
java -jar target/gestion-naissances-1.0.0.jar
```

---

## 🗄️ Schéma de la Base de Données Locale (SQLite)

La base `naissances.db` est automatiquement initialisée au premier lancement avec la table `declarations_naissance` :

| Colonne | Type | Description |
|---|---|---|
| `id` | `INTEGER` | Identifiant unique auto-incrémenté |
| `numero_acte` | `TEXT UNIQUE` | Numéro officiel (ex: `ACTE-2025-0001`) |
| `annee_registre` | `INTEGER` | Année du registre d'état civil |
| `date_enregistrement` | `TEXT` | Date de déclaration (format ISO `YYYY-MM-DD`) |
| `nom_enfant` | `TEXT` | Nom de famille de l'enfant |
| `prenoms_enfant` | `TEXT` | Prénoms de l'enfant |
| `sexe` | `TEXT` | `M` (Masculin) ou `F` (Féminin) |
| `date_naissance` | `TEXT` | Date de naissance |
| `heure_naissance` | `TEXT` | Heure de naissance |
| `lieu_naissance` | `TEXT` | Lieu ou maternité |
| `nom_pere`, `prenom_pere` | `TEXT` | Identité du père |
| `nom_mere`, `prenom_mere` | `TEXT` | Identité de la mère |
| `nom_declarant` | `TEXT` | Nom du déclarant |
| `qualite_declarant` | `TEXT` | Père, Mère, Médecin, Sage-femme, etc. |
| `officier_etat_civil` | `TEXT` | Nom et fonction de l'officier |
| `observations` | `TEXT` | Mentions marginales ou remarques |
| `date_creation` | `TIMESTAMP` | Horodatage de création de l'enregistrement |

---

## 🔒 Fonctionnement 100% Hors-Ligne & Sécurité

- Aucun appel réseau, aucune télémétrie, aucun cloud externe requis.
- Les données sont stockées localement dans `naissances.db`.
- Sauvegarde ultra-simple : il suffit de copier le fichier `naissances.db` sur clé USB ou disque externe pour archivage pérenne.
