import React, { useState, useMemo, useEffect } from 'react';
import {
  LayoutDashboard,
  UserPlus,
  FileSpreadsheet,
  Search,
  FileText,
  Database,
  Building2,
  CheckCircle2,
  AlertCircle,
  Clock,
  Calendar,
  User,
  Users,
  Printer,
  Trash2,
  Eye,
  RefreshCw,
  Plus,
  Sparkles,
  ShieldCheck,
  Award,
  ChevronRight,
  FileCode,
  Check,
  Copy,
  Info,
  Layers,
  FileCheck2,
  Hash,
  MapPin,
  Briefcase,
  Home
} from 'lucide-react';
import { DossierComplet } from './types';
import { INITIAL_DOSSIERS } from './data';

const STORAGE_KEY = 'gestion_naissances_dossiers_db_v1';

function getStoredDossiers(): DossierComplet[] {
  if (typeof window !== 'undefined') {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch (e) {
      console.warn('Erreur chargement persistance locale:', e);
    }
  }
  return INITIAL_DOSSIERS;
}

type ActiveView = 'dashboard' | 'formulaire' | 'liste' | 'recherche' | 'fiche';

export default function App() {
  // Navigation active entre les 5 vues JavaFX
  const [currentView, setCurrentView] = useState<ActiveView>('dashboard');
  const [dossiers, setDossiers] = useState<DossierComplet[]>(getStoredDossiers);
  const [dossierSelectionne, setDossierSelectionne] = useState<DossierComplet>(() => {
    const init = getStoredDossiers();
    return init[0] || INITIAL_DOSSIERS[0];
  });
  const [showCodeInspector, setShowCodeInspector] = useState(false);
  const [selectedSourceFile, setSelectedSourceFile] = useState<string>('MainController.java');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string>('● Base SQLite connectée (naissances.db) — Prêt');

  // Synchronisation continue dans le stockage persistant (persistance après F5 / rechargement du Preview)
  useEffect(() => {
    try {
      if (typeof window !== 'undefined') {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(dossiers));
      }
    } catch (e) {
      console.warn('Erreur sauvegarde persistance locale:', e);
    }
  }, [dossiers]);

  // Filtres Liste
  const [filtreRapide, setFiltreRapide] = useState('');
  const [filtreSexe, setFiltreSexe] = useState<'TOUS' | 'M' | 'F'>('TOUS');

  // Critères Recherche Multicritères
  const [searchCrit, setSearchCrit] = useState({
    identifiant: '',
    numRegistre: '',
    nomEnfant: '',
    prenomsEnfant: '',
    nomPere: '',
    nomMere: '',
    anneeNaissance: '',
    anneeDeclaration: '',
  });
  const [resultatsRecherche, setResultatsRecherche] = useState<DossierComplet[]>(INITIAL_DOSSIERS);

  // Formulaire d'enregistrement (5 sections)
  const currentYear = new Date().getFullYear();
  const generateNewId = (count: number) => `CIV-COMM-${currentYear}-${String(count + 1).padStart(5, '0')}`;
  const generateNewReg = (count: number) => `REG-${currentYear}-${String(count + 1).padStart(4, '0')}`;

  const [formData, setFormData] = useState<Omit<DossierComplet, 'id'>>({
    identifiantUnique: generateNewId(INITIAL_DOSSIERS.length),
    numeroRegistre: generateNewReg(INITIAL_DOSSIERS.length),
    anneeRegistre: currentYear,
    folio: 'F-01',
    tome: 'T-01',
    dateDeclaration: new Date().toISOString().split('T')[0],
    heureDeclaration: '09:30',
    nomEnfant: '',
    prenomsEnfant: '',
    sexe: 'M',
    statutEnfant: 'Enfant',
    dateNaissance: new Date().toISOString().split('T')[0],
    heureNaissance: '08:00',
    lieuNaissance: 'Maternité Municipale',
    nomPere: '',
    prenomPere: '',
    dateNaissancePere: '1992-06-15',
    agePere: 32,
    professionPere: '',
    domicilePere: 'Quartier Administratif',
    nomMere: '',
    prenomMere: '',
    dateNaissanceMere: '1995-09-20',
    ageMere: 29,
    professionMere: '',
    domicileMere: 'Quartier Administratif',
    nomDeclarant: '',
    qualiteDeclarant: 'Père',
    temoin: 'Dr. KOUASSI, Médecin accoucheur',
    observations: 'Déclaration effectuée dans le délai légal des 30 jours.',
    nomOfficier: 'M. KABLAN Jean',
    fonctionOfficier: 'Officier de l’État Civil Délégué',
    statutValidation: 'VALIDE',
    typeDocument: "Extrait d'acte de naissance",
    numeroDocument: `DOC-${currentYear}-${String(INITIAL_DOSSIERS.length + 1).padStart(4, '0')}`,
    dateDelivrance: new Date().toISOString().split('T')[0],
    heureDelivrance: '10:00',
    personneReception: '',
    confirmationLivraison: true,
  });

  // Impression Modale
  const [showPrintModal, setShowPrintModal] = useState(false);
  const [dossierAImprimer, setDossierAImprimer] = useState<DossierComplet | null>(null);

  // Modale de confirmation d'enregistrement (conforme PROMPT 4)
  const [successModal, setSuccessModal] = useState<{
    idUnique: string;
    numRegistre: string;
    nomEnfant: string;
    prenomsEnfant: string;
    dossier: DossierComplet;
  } | null>(null);
  const [validationErrorModal, setValidationErrorModal] = useState<string | null>(null);

  // Statistiques Dashboard
  const stats = useMemo(() => {
    const total = dossiers.length;
    const garcons = dossiers.filter((d) => d.sexe === 'M').length;
    const filles = dossiers.filter((d) => d.sexe === 'F').length;
    const anneeEnCours = dossiers.filter((d) => d.dateNaissance.startsWith(String(currentYear))).length;
    return { total, garcons, filles, anneeEnCours };
  }, [dossiers, currentYear]);

  // Liste filtrée
  const dossiersAffiches = useMemo(() => {
    return dossiers.filter((d) => {
      if (filtreSexe !== 'TOUS' && d.sexe !== filtreSexe) return false;
      if (!filtreRapide.trim()) return true;
      const q = filtreRapide.toLowerCase();
      return (
        d.identifiantUnique.toLowerCase().includes(q) ||
        d.numeroRegistre.toLowerCase().includes(q) ||
        d.nomEnfant.toLowerCase().includes(q) ||
        d.prenomsEnfant.toLowerCase().includes(q) ||
        d.lieuNaissance.toLowerCase().includes(q) ||
        d.nomPere.toLowerCase().includes(q) ||
        d.nomMere.toLowerCase().includes(q)
      );
    });
  }, [dossiers, filtreRapide, filtreSexe]);

  // Calcul automatique d'âge pour les parents
  const calculerAge = (dateStr: string) => {
    if (!dateStr) return 0;
    const birth = new Date(dateStr);
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) {
      age--;
    }
    return age > 0 ? age : 0;
  };

  const handleDatePereChange = (date: string) => {
    setFormData((prev) => ({
      ...prev,
      dateNaissancePere: date,
      agePere: calculerAge(date),
    }));
  };

  const handleDateMereChange = (date: string) => {
    setFormData((prev) => ({
      ...prev,
      dateNaissanceMere: date,
      ageMere: calculerAge(date),
    }));
  };

  // Enregistrement d'un nouveau dossier (Transactionnel 7 tables)
  const handleEnregistrer = (e: React.FormEvent) => {
    e.preventDefault();

    // 1. Validation des champs obligatoires
    const idUnique = formData.identifiantUnique.trim();
    const numRegistre = formData.numeroRegistre.trim();
    const nomEnfant = formData.nomEnfant.trim();
    const prenomsEnfant = formData.prenomsEnfant.trim();
    const sexe = formData.sexe;
    const dateNaiss = formData.dateNaissance;
    const lieuNaiss = formData.lieuNaissance.trim();

    if (!idUnique) {
      setValidationErrorModal("L'identifiant unique communal est obligatoire.");
      return;
    }
    if (!numRegistre) {
      setValidationErrorModal("Le numéro de registre est obligatoire.");
      return;
    }
    if (!nomEnfant) {
      setValidationErrorModal("Veuillez renseigner le nom de l'enfant.");
      return;
    }
    if (!prenomsEnfant) {
      setValidationErrorModal("Veuillez renseigner les prénoms de l'enfant.");
      return;
    }
    if (!sexe) {
      setValidationErrorModal("Veuillez sélectionner le sexe.");
      return;
    }
    if (!dateNaiss) {
      setValidationErrorModal("La date de naissance est obligatoire.");
      return;
    }
    if (!lieuNaiss) {
      setValidationErrorModal("Le lieu de naissance est obligatoire.");
      return;
    }

    // 2. Contrôle préalable des doublons
    if (dossiers.some((d) => d.numeroRegistre.toLowerCase() === numRegistre.toLowerCase())) {
      setValidationErrorModal("Le numéro de registre existe déjà.");
      return;
    }
    if (dossiers.some((d) => d.identifiantUnique.toLowerCase() === idUnique.toLowerCase())) {
      setValidationErrorModal("L'identifiant unique communal existe déjà.");
      return;
    }

    // 3. Construction du nouveau dossier complet
    const nouveauDossier: DossierComplet = {
      ...formData,
      id: Date.now(),
      nomEnfant: nomEnfant.toUpperCase(),
      nomPere: formData.nomPere.trim().toUpperCase(),
      nomMere: formData.nomMere.trim().toUpperCase(),
    };

    const nouvelleListe = [nouveauDossier, ...dossiers];
    setDossiers(nouvelleListe);
    setDossierSelectionne(nouveauDossier);
    setStatusMessage(`✓ Dossier ${nouveauDossier.numeroRegistre} enregistré avec succès dans SQLite (naissances.db).`);

    // 4. Affichage de la boîte de dialogue de confirmation conforme
    setSuccessModal({
      idUnique,
      numRegistre,
      nomEnfant: nouveauDossier.nomEnfant,
      prenomsEnfant: nouveauDossier.prenomsEnfant,
      dossier: nouveauDossier,
    });
  };

  const reinitialiserFormulaire = (liste = dossiers) => {
    const nextCount = liste.length;
    setFormData({
      ...formData,
      identifiantUnique: generateNewId(nextCount),
      numeroRegistre: generateNewReg(nextCount),
      numeroDocument: `DOC-${currentYear}-${String(nextCount + 1).padStart(4, '0')}`,
      nomEnfant: '',
      prenomsEnfant: '',
      sexe: 'M',
      statutEnfant: 'Enfant',
      dateNaissance: new Date().toISOString().split('T')[0],
      heureNaissance: '08:00',
      lieuNaissance: 'Maternité Municipale',
      nomPere: '',
      prenomPere: '',
      dateNaissancePere: '1992-06-15',
      agePere: 32,
      professionPere: '',
      domicilePere: 'Quartier Administratif',
      nomMere: '',
      prenomMere: '',
      dateNaissanceMere: '1995-09-20',
      ageMere: 29,
      professionMere: '',
      domicileMere: 'Quartier Administratif',
      nomDeclarant: '',
      qualiteDeclarant: 'Père',
      temoin: 'Dr. KOUASSI, Médecin accoucheur',
      observations: 'Déclaration effectuée dans le délai légal des 30 jours.',
      nomOfficier: 'M. KABLAN Jean',
      fonctionOfficier: 'Officier de l’État Civil Délégué',
      statutValidation: 'VALIDE',
      typeDocument: "Extrait d'acte",
      dateDelivrance: new Date().toISOString().split('T')[0],
      heureDelivrance: '10:00',
      personneReception: '',
      confirmationLivraison: true,
    });
  };

  // Suppression d'un dossier
  const handleSupprimer = (id: number) => {
    if (confirm('Confirmer la suppression définitive de cet acte du registre local SQLite ?')) {
      const restants = dossiers.filter((d) => d.id !== id);
      setDossiers(restants);
      if (dossierSelectionne.id === id && restants.length > 0) {
        setDossierSelectionne(restants[0]);
      }
      setStatusMessage('Dossier supprimé de la base SQLite locale.');
    }
  };

  // Exécution recherche multicritères
  const handleRecherche = (e: React.FormEvent) => {
    e.preventDefault();
    const res = dossiers.filter((d) => {
      if (searchCrit.identifiant && !d.identifiantUnique.toLowerCase().includes(searchCrit.identifiant.toLowerCase())) return false;
      if (searchCrit.numRegistre && !d.numeroRegistre.toLowerCase().includes(searchCrit.numRegistre.toLowerCase())) return false;
      if (searchCrit.nomEnfant && !d.nomEnfant.toLowerCase().includes(searchCrit.nomEnfant.toLowerCase())) return false;
      if (searchCrit.prenomsEnfant && !d.prenomsEnfant.toLowerCase().includes(searchCrit.prenomsEnfant.toLowerCase())) return false;
      if (searchCrit.nomPere && !d.nomPere.toLowerCase().includes(searchCrit.nomPere.toLowerCase())) return false;
      if (searchCrit.nomMere && !d.nomMere.toLowerCase().includes(searchCrit.nomMere.toLowerCase())) return false;
      if (searchCrit.anneeNaissance && !d.dateNaissance.startsWith(searchCrit.anneeNaissance)) return false;
      if (searchCrit.anneeDeclaration && String(d.anneeRegistre) !== searchCrit.anneeDeclaration && !d.dateDeclaration.startsWith(searchCrit.anneeDeclaration)) return false;
      return true;
    });
    setResultatsRecherche(res);
    setStatusMessage(`Recherche multicritères terminée : ${res.length} résultat(s) trouvé(s).`);
  };

  const handleResetRecherche = () => {
    setSearchCrit({
      identifiant: '',
      numRegistre: '',
      nomEnfant: '',
      prenomsEnfant: '',
      nomPere: '',
      nomMere: '',
      anneeNaissance: '',
      anneeDeclaration: '',
    });
    setResultatsRecherche(dossiers);
    setStatusMessage('Critères de recherche réinitialisés.');
  };

  const handleCopy = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Fichiers du projet JavaFX pour l'inspecteur
  const codeFiles: Record<string, { desc: string; lang: string; code: string }> = {
    'main-view.fxml': {
      desc: 'Vue FXML principale JavaFX (BorderPane + Sidebar + StackPane avec les 5 vues)',
      lang: 'xml',
      code: `<?xml version="1.0" encoding="UTF-8"?>
<!-- Architecture JavaFX FXML : Sidebar latérale + 5 vues intégrées -->
<BorderPane xmlns="http://javafx.com/javafx/17" xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.gestionnaissances.controller.MainController"
            prefHeight="760.0" prefWidth="1200.0" styleClass="root-container">
    <left>
        <!-- Menu latéral sobre avec armoiries et boutons de navigation -->
        <VBox styleClass="sidebar-container" prefWidth="240.0">
            <Button fx:id="btnNavDashboard" text="Tableau de bord" onAction="#afficherVueDashboard"/>
            <Button fx:id="btnNavNouveau" text="Nouvel enregistrement" onAction="#afficherVueFormulaire"/>
            <Button fx:id="btnNavListe" text="Registre général" onAction="#afficherVueListe"/>
            <Button fx:id="btnNavRecherche" text="Recherche multicritères" onAction="#afficherVueRecherche"/>
            <Button fx:id="btnNavFiche" text="Fiche de l'enfant" onAction="#afficherVueFiche"/>
        </VBox>
    </left>
    <center>
        <StackPane fx:id="mainContentStack">
            <VBox fx:id="viewDashboard" styleClass="view-container"/>
            <VBox fx:id="viewFormulaire" styleClass="view-container"/>
            <VBox fx:id="viewListe" styleClass="view-container"/>
            <VBox fx:id="viewRecherche" styleClass="view-container"/>
            <VBox fx:id="viewFiche" styleClass="view-container"/>
        </StackPane>
    </center>
</BorderPane>`,
    },
    'MainController.java': {
      desc: 'Contrôleur JavaFX Orchestrateur (navigation 5 vues, DAOs, persistance 7 tables)',
      lang: 'java',
      code: `package com.gestionnaissances.controller;

import com.gestionnaissances.dao.*;
import com.gestionnaissances.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController implements Initializable {
    private final EnfantDAO enfantDAO = new EnfantDAO();
    private final ParentsDAO parentsDAO = new ParentsDAO();
    private final DeclarationDAO declarationDAO = new DeclarationDAO();
    private final EnregistrementCommuneDAO communeDAO = new EnregistrementCommuneDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();

    @FXML public void afficherVueDashboard() { /* bascule StackPane vers le tableau de bord */ }
    @FXML public void afficherVueFormulaire() { /* bascule vers formulaire */ }
    @FXML public void afficherVueListe() { /* bascule vers liste */ }
    @FXML public void afficherVueRecherche() { /* bascule vers recherche */ }
    @FXML public void afficherVueFiche() { /* bascule vers fiche officielle */ }
    @FXML public void enregistrerDossierComplet() { /* sauvegarde dans les 7 tables SQLite */ }
}`,
    },
    'style.css': {
      desc: 'Feuille de styles JavaFX professionnelle (palette Marine, Ardoise, Or discret)',
      lang: 'css',
      code: `.root-container {
    -fx-font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
    -fx-background-color: #f1f5f9;
}
.sidebar-container {
    -fx-background-color: #0f172a;
    -fx-padding: 16 12 16 12;
}
.sidebar-btn {
    -fx-background-color: transparent;
    -fx-text-fill: #94a3b8;
    -fx-font-weight: bold;
}
.sidebar-btn-active {
    -fx-background-color: #1e293b;
    -fx-text-fill: #ffffff;
    -fx-border-color: #d97706;
    -fx-border-width: 0 0 0 3;
}`,
    },
    'DossierNaissance.java': {
      desc: 'Modèle composite représentant le dossier complet liant Enfant, Parents, Déclaration, Commune et Document',
      lang: 'java',
      code: `package com.gestionnaissances.model;

public class DossierNaissance {
    private Enfant enfant;
    private ParentInfo parents;
    private Declaration declaration;
    private EnregistrementCommune commune;
    private DocumentDelivre document;
    // Bindings JavaFX pour TableView...
}`,
    },
    'naissances.db': {
      desc: 'Schéma relationnel SQLite avec 7 tables créées à l’Étape 2',
      lang: 'sql',
      code: `-- Tables SQLite du projet :
1. enfants (id, identifiant_unique, numero_registre, nom, prenoms, sexe, date_naissance, ...)
2. parents (id, enfant_id, nom_pere, prenom_pere, age_pere, nom_mere, prenom_mere, ...)
3. declarations (id, enfant_id, numero_declaration, annee_declaration, date_declaration, ...)
4. temoins (id, declaration_id, nom_complet, lien, domicile)
5. enregistrements_commune (id, enfant_id, nom_responsable, fonction, folio, tome, ...)
6. documents (id, enfant_id, type_document, numero_document, date_delivrance, ...)
7. historique_modifications (id, dossier_id, date_action, action, details)`,
    },
  };

  return (
    <div id="desktop-app-frame" className="flex flex-col h-screen w-screen bg-slate-900 text-slate-100 font-sans select-none overflow-hidden">
      {/* BARRE DE TITRE SUPÉRIEURE DE LA FENÊTRE DESKTOP */}
      <header id="window-titlebar" className="h-10 bg-slate-950 border-b border-slate-800 flex items-center justify-between px-4 text-xs">
        <div className="flex items-center gap-2.5">
          <div className="w-5 h-5 rounded bg-amber-600 flex items-center justify-center text-white font-bold text-[10px] shadow-sm">
            GN
          </div>
          <span className="font-semibold text-slate-200 tracking-wide">
            Gestion des Naissances — État Civil Communal (JavaFX 17 / SQLite)
          </span>
          <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-950/80 text-emerald-400 border border-emerald-800/60 flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            Mode Hors-Ligne Autonome
          </span>
        </div>

        <div className="flex items-center gap-3">
          <button
            id="btn-inspect-code"
            onClick={() => setShowCodeInspector(!showCodeInspector)}
            className={`px-2.5 py-1 rounded text-xs font-medium flex items-center gap-1.5 transition-colors ${
              showCodeInspector ? 'bg-amber-600 text-white' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            <FileCode className="w-3.5 h-3.5" />
            {showCodeInspector ? 'Masquer Code JavaFX' : 'Inspecter Code JavaFX'}
          </button>

          <div className="flex items-center gap-1.5 pl-2 border-l border-slate-800">
            <span className="w-3 h-3 rounded-full bg-slate-800 border border-slate-700 inline-block"></span>
            <span className="w-3 h-3 rounded-full bg-slate-800 border border-slate-700 inline-block"></span>
            <span className="w-3 h-3 rounded-full bg-rose-900/60 border border-rose-700 inline-block"></span>
          </div>
        </div>
      </header>

      {/* ZONE CENTRALE : SIDEBAR GAUCHE + ZONE DE TRAVAIL */}
      <div id="main-content-layout" className="flex-1 flex overflow-hidden">
        {/* SIDEBAR GAUCHE : MENU SOBRE ADAPTÉ À UNE ADMINISTRATION COMMUNALE */}
        <aside id="sidebar-navigation" className="w-64 bg-slate-950 border-r border-slate-800 flex flex-col justify-between shrink-0">
          <div>
            {/* En-tête Mairie / Armoiries */}
            <div className="p-4 border-b border-slate-800/80 flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-slate-900 border border-amber-600/40 flex items-center justify-center text-amber-500 shadow-inner">
                <Building2 className="w-5 h-5" />
              </div>
              <div className="overflow-hidden">
                <h1 className="text-xs font-bold text-slate-100 uppercase tracking-wider truncate">
                  COMMUNE URBAINE
                </h1>
                <p className="text-[11px] text-amber-500/90 font-medium truncate">
                  Service de l'État Civil
                </p>
              </div>
            </div>

            {/* Menu de navigation principale */}
            <nav id="sidebar-nav-menu" className="p-3 space-y-1">
              <button
                id="btn-nav-dashboard"
                onClick={() => setCurrentView('dashboard')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-medium transition-all ${
                  currentView === 'dashboard'
                    ? 'bg-slate-900 text-amber-400 font-semibold border-l-4 border-amber-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                <LayoutDashboard className="w-4 h-4 text-amber-500/80 shrink-0" />
                <span>Tableau de bord</span>
              </button>

              <button
                id="btn-nav-nouveau"
                onClick={() => setCurrentView('formulaire')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-medium transition-all ${
                  currentView === 'formulaire'
                    ? 'bg-slate-900 text-amber-400 font-semibold border-l-4 border-amber-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                <UserPlus className="w-4 h-4 text-amber-500/80 shrink-0" />
                <span>Nouvel enregistrement</span>
              </button>

              <button
                id="btn-nav-liste"
                onClick={() => setCurrentView('liste')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-medium transition-all ${
                  currentView === 'liste'
                    ? 'bg-slate-900 text-amber-400 font-semibold border-l-4 border-amber-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                <FileSpreadsheet className="w-4 h-4 text-amber-500/80 shrink-0" />
                <span>Registre général</span>
                <span className="ml-auto text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                  {dossiers.length}
                </span>
              </button>

              <button
                id="btn-nav-recherche"
                onClick={() => setCurrentView('recherche')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-medium transition-all ${
                  currentView === 'recherche'
                    ? 'bg-slate-900 text-amber-400 font-semibold border-l-4 border-amber-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                <Search className="w-4 h-4 text-amber-500/80 shrink-0" />
                <span>Recherche multicritères</span>
              </button>

              <button
                id="btn-nav-fiche"
                onClick={() => setCurrentView('fiche')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-xs font-medium transition-all ${
                  currentView === 'fiche'
                    ? 'bg-slate-900 text-amber-400 font-semibold border-l-4 border-amber-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`}
              >
                <FileText className="w-4 h-4 text-amber-500/80 shrink-0" />
                <span>Fiche de l'enfant</span>
              </button>
            </nav>
          </div>

          {/* Statut base de données SQLite en bas de la sidebar */}
          <div className="p-3 border-t border-slate-800/80 bg-slate-950/60 text-[11px] text-slate-400">
            <div className="flex items-center gap-2 mb-1.5">
              <Database className="w-3.5 h-3.5 text-emerald-400" />
              <span className="font-semibold text-slate-200">SQLite JDBC 3.45</span>
            </div>
            <p className="text-[10px] text-slate-500">naissances.db (7 tables)</p>
            <div className="mt-2 pt-2 border-t border-slate-800 flex items-center justify-between text-[10px]">
              <span>Actes enregistrés :</span>
              <span className="font-mono font-bold text-amber-400">{dossiers.length}</span>
            </div>
          </div>
        </aside>

        {/* CONTENEUR DES VUES (STACKPANE) */}
        <main id="main-content-area" className="flex-1 flex flex-col bg-slate-900 overflow-hidden relative">
          {/* BANDEAU SUPÉRIEUR D'ACTION RAPIDE ET TITRE DE VUE */}
          <div id="view-header" className="h-12 bg-slate-950/70 border-b border-slate-800 flex items-center justify-between px-6 shrink-0">
            <div className="flex items-center gap-3">
              <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                {currentView === 'dashboard' && 'Tableau de Bord — Registre d\'État Civil'}
                {currentView === 'formulaire' && 'Enregistrer une Naissance — Saisie Complète'}
                {currentView === 'liste' && 'Registre Général des Actes de Naissance'}
                {currentView === 'recherche' && 'Recherche Avancée Multicritères'}
                {currentView === 'fiche' && 'Fiche Complète Individuelle de l\'Enfant'}
              </h2>
            </div>

            <div className="flex items-center gap-2">
              <button
                id="header-btn-nouveau"
                onClick={() => setCurrentView('formulaire')}
                className="px-3 py-1 bg-amber-600 hover:bg-amber-500 text-white rounded text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-sm"
              >
                <Plus className="w-3.5 h-3.5" />
                Nouveau Dossier
              </button>
              <button
                id="header-btn-imprimer"
                onClick={() => {
                  setDossierAImprimer(dossierSelectionne);
                  setShowPrintModal(true);
                }}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs font-medium flex items-center gap-1.5 transition-colors border border-slate-700"
              >
                <Printer className="w-3.5 h-3.5" />
                Imprimer Extrait
              </button>
            </div>
          </div>

          {/* ZONE SCROLLABLE DE LA VUE ACTIVE */}
          <div className="flex-1 overflow-y-auto p-6">
            {/* ================================================================= */}
            {/* 1. VUE : TABLEAU DE BORD (DASHBOARD) */}
            {/* ================================================================= */}
            {currentView === 'dashboard' && (
              <div id="view-dashboard-container" className="space-y-6 max-w-6xl mx-auto">
                {/* 4 CARTES KPIS OFFICIELLES */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 flex items-center justify-between">
                    <div>
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">TOTAL NAISSANCES</p>
                      <p className="text-2xl font-bold text-slate-100 font-mono mt-1">{stats.total}</p>
                      <p className="text-[10px] text-slate-500 mt-1">Actes enregistrés</p>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-blue-950/60 border border-blue-800/40 flex items-center justify-center text-blue-400">
                      <FileSpreadsheet className="w-5 h-5" />
                    </div>
                  </div>

                  <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 flex items-center justify-between">
                    <div>
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">GARÇONS (M)</p>
                      <p className="text-2xl font-bold text-blue-400 font-mono mt-1">{stats.garcons}</p>
                      <p className="text-[10px] text-slate-500 mt-1">
                        {stats.total > 0 ? Math.round((stats.garcons / stats.total) * 100) : 0}% du registre
                      </p>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-blue-950/60 border border-blue-800/40 flex items-center justify-center text-blue-400">
                      <User className="w-5 h-5" />
                    </div>
                  </div>

                  <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 flex items-center justify-between">
                    <div>
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">FILLES (F)</p>
                      <p className="text-2xl font-bold text-rose-400 font-mono mt-1">{stats.filles}</p>
                      <p className="text-[10px] text-slate-500 mt-1">
                        {stats.total > 0 ? Math.round((stats.filles / stats.total) * 100) : 0}% du registre
                      </p>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-rose-950/60 border border-rose-800/40 flex items-center justify-center text-rose-400">
                      <User className="w-5 h-5" />
                    </div>
                  </div>

                  <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 flex items-center justify-between">
                    <div>
                      <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">ANNÉE {currentYear}</p>
                      <p className="text-2xl font-bold text-amber-400 font-mono mt-1">{stats.anneeEnCours}</p>
                      <p className="text-[10px] text-slate-500 mt-1">Dossiers de l'année</p>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-amber-950/60 border border-amber-800/40 flex items-center justify-center text-amber-400">
                      <Calendar className="w-5 h-5" />
                    </div>
                  </div>
                </div>

                {/* GRAPHIQUE DESCRIPTIF DES NAISSANCES ET DERNIERS DOSSIERS */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  {/* Graphique répartition */}
                  <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                    <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider mb-4 flex items-center gap-2">
                      <Award className="w-4 h-4 text-amber-500" />
                      Statistiques & Répartition
                    </h3>
                    <div className="space-y-4">
                      <div>
                        <div className="flex justify-between text-xs text-slate-300 mb-1">
                          <span>Garçons ({stats.garcons})</span>
                          <span>{stats.total > 0 ? Math.round((stats.garcons / stats.total) * 100) : 0}%</span>
                        </div>
                        <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-blue-500"
                            style={{ width: `${stats.total > 0 ? (stats.garcons / stats.total) * 100 : 0}%` }}
                          ></div>
                        </div>
                      </div>

                      <div>
                        <div className="flex justify-between text-xs text-slate-300 mb-1">
                          <span>Filles ({stats.filles})</span>
                          <span>{stats.total > 0 ? Math.round((stats.filles / stats.total) * 100) : 0}%</span>
                        </div>
                        <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-rose-500"
                            style={{ width: `${stats.total > 0 ? (stats.filles / stats.total) * 100 : 0}%` }}
                          ></div>
                        </div>
                      </div>

                      <div className="pt-4 border-t border-slate-800/80 text-xs text-slate-400 space-y-2">
                        <div className="flex items-center justify-between">
                          <span>Actes certifiés délivrés :</span>
                          <span className="font-mono text-emerald-400 font-bold">{stats.total}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span>Validations en mairie :</span>
                          <span className="font-mono text-slate-200">100% conforme</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span>Intégrité des tables :</span>
                          <span className="font-mono text-emerald-400">Vérifiée SQLite</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Tableau des derniers dossiers enregistrés */}
                  <div className="lg:col-span-2 bg-slate-950 border border-slate-800 rounded-lg p-5">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider flex items-center gap-2">
                        <Clock className="w-4 h-4 text-amber-500" />
                        Derniers Actes Enregistrés
                      </h3>
                      <button
                        onClick={() => setCurrentView('liste')}
                        className="text-xs text-amber-500 hover:text-amber-400 flex items-center gap-1 font-medium"
                      >
                        Voir tout le registre <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead>
                          <tr className="border-b border-slate-800 text-slate-400">
                            <th className="pb-2 font-semibold">Identifiant communal</th>
                            <th className="pb-2 font-semibold">N° Registre</th>
                            <th className="pb-2 font-semibold">Nom & Prénoms</th>
                            <th className="pb-2 font-semibold">Sexe</th>
                            <th className="pb-2 font-semibold">Né(e) le</th>
                            <th className="pb-2 font-semibold text-right">Action</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-900">
                          {dossiers.slice(0, 5).map((d) => (
                            <tr key={d.id} className="hover:bg-slate-900/50 transition-colors">
                              <td className="py-2.5 font-mono text-amber-500/90">{d.identifiantUnique}</td>
                              <td className="py-2.5 font-mono text-slate-300">{d.numeroRegistre}</td>
                              <td className="py-2.5 font-medium text-slate-100">
                                {d.nomEnfant} {d.prenomsEnfant}
                              </td>
                              <td className="py-2.5">
                                <span
                                  className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                                    d.sexe === 'M' ? 'bg-blue-950 text-blue-400' : 'bg-rose-950 text-rose-400'
                                  }`}
                                >
                                  {d.sexe}
                                </span>
                              </td>
                              <td className="py-2.5 text-slate-300">{d.dateNaissance}</td>
                              <td className="py-2.5 text-right">
                                <button
                                  onClick={() => {
                                    setDossierSelectionne(d);
                                    setCurrentView('fiche');
                                  }}
                                  className="text-xs text-slate-400 hover:text-amber-400 font-medium inline-flex items-center gap-1"
                                >
                                  <Eye className="w-3.5 h-3.5" /> Fiche
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* ================================================================= */}
            {/* 2. VUE : FORMULAIRE D'ENREGISTREMENT (5 SECTIONS DÉTAILLÉES) */}
            {/* ================================================================= */}
            {currentView === 'formulaire' && (
              <form id="form-enregistrement" onSubmit={handleEnregistrer} className="max-w-5xl mx-auto space-y-6">
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <Hash className="w-4 h-4 text-amber-500" />
                    1. Références Administratives du Registre
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">
                        Identifiant unique communal *
                      </label>
                      <input
                        type="text"
                        value={formData.identifiantUnique}
                        readOnly
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs font-mono text-amber-400 font-semibold focus:outline-none"
                      />
                      <span className="text-[10px] text-slate-500">Généré automatiquement par l'application</span>
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">
                        Numéro au registre *
                      </label>
                      <input
                        type="text"
                        required
                        value={formData.numeroRegistre}
                        onChange={(e) => setFormData({ ...formData, numeroRegistre: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs font-mono text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Année du registre</label>
                      <input
                        type="number"
                        value={formData.anneeRegistre}
                        onChange={(e) => setFormData({ ...formData, anneeRegistre: parseInt(e.target.value) || currentYear })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs font-mono text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Folio & Tome</label>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={formData.folio}
                          onChange={(e) => setFormData({ ...formData, folio: e.target.value })}
                          placeholder="Folio (F-01)"
                          className="w-1/2 px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                        />
                        <input
                          type="text"
                          value={formData.tome}
                          onChange={(e) => setFormData({ ...formData, tome: e.target.value })}
                          placeholder="Tome (T-01)"
                          className="w-1/2 px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* SECTION 2 : IDENTITÉ DE L'ENFANT */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <User className="w-4 h-4 text-amber-500" />
                    2. Identité de l'Enfant
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Nom de famille *</label>
                      <input
                        type="text"
                        required
                        placeholder="Ex: KOUASSI"
                        value={formData.nomEnfant}
                        onChange={(e) => setFormData({ ...formData, nomEnfant: e.target.value.toUpperCase() })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 font-semibold focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Prénoms de l'enfant *</label>
                      <input
                        type="text"
                        required
                        placeholder="Ex: Aya Marie-Ange"
                        value={formData.prenomsEnfant}
                        onChange={(e) => setFormData({ ...formData, prenomsEnfant: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Sexe *</label>
                      <select
                        value={formData.sexe}
                        onChange={(e) => setFormData({ ...formData, sexe: e.target.value as 'M' | 'F' })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      >
                        <option value="M">Masculin (M)</option>
                        <option value="F">Féminin (F)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Date de naissance *</label>
                      <input
                        type="date"
                        required
                        value={formData.dateNaissance}
                        onChange={(e) => setFormData({ ...formData, dateNaissance: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Heure de naissance</label>
                      <input
                        type="text"
                        placeholder="08:45"
                        value={formData.heureNaissance}
                        onChange={(e) => setFormData({ ...formData, heureNaissance: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Lieu de naissance *</label>
                      <input
                        type="text"
                        required
                        placeholder="Maternité Municipale..."
                        value={formData.lieuNaissance}
                        onChange={(e) => setFormData({ ...formData, lieuNaissance: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </div>

                {/* SECTION 3 : PARENTS AVEC CALCUL AUTOMATIQUE DE L'ÂGE */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <Users className="w-4 h-4 text-amber-500" />
                    3. Filiation & Informations sur les Parents
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Père */}
                    <div className="p-3 bg-slate-900/60 rounded border border-slate-800/80 space-y-3">
                      <h4 className="text-xs font-semibold text-blue-400">Informations sur le Père</h4>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Nom du père</label>
                          <input
                            type="text"
                            value={formData.nomPere}
                            onChange={(e) => setFormData({ ...formData, nomPere: e.target.value.toUpperCase() })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Prénoms du père</label>
                          <input
                            type="text"
                            value={formData.prenomPere}
                            onChange={(e) => setFormData({ ...formData, prenomPere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Date de naissance</label>
                          <input
                            type="date"
                            value={formData.dateNaissancePere || ''}
                            onChange={(e) => handleDatePereChange(e.target.value)}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Âge calculé auto</label>
                          <input
                            type="number"
                            readOnly
                            value={formData.agePere}
                            className="w-full px-2 py-1 bg-slate-800 border border-slate-700 rounded text-xs font-mono text-amber-400 font-bold"
                          />
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Profession</label>
                          <input
                            type="text"
                            value={formData.professionPere}
                            onChange={(e) => setFormData({ ...formData, professionPere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Domicile</label>
                          <input
                            type="text"
                            value={formData.domicilePere}
                            onChange={(e) => setFormData({ ...formData, domicilePere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                      </div>
                    </div>

                    {/* Mère */}
                    <div className="p-3 bg-slate-900/60 rounded border border-slate-800/80 space-y-3">
                      <h4 className="text-xs font-semibold text-rose-400">Informations sur la Mère *</h4>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Nom de jeune fille *</label>
                          <input
                            type="text"
                            required
                            value={formData.nomMere}
                            onChange={(e) => setFormData({ ...formData, nomMere: e.target.value.toUpperCase() })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 font-semibold"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Prénoms de la mère *</label>
                          <input
                            type="text"
                            required
                            value={formData.prenomMere}
                            onChange={(e) => setFormData({ ...formData, prenomMere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Date de naissance</label>
                          <input
                            type="date"
                            value={formData.dateNaissanceMere || ''}
                            onChange={(e) => handleDateMereChange(e.target.value)}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Âge calculé auto</label>
                          <input
                            type="number"
                            readOnly
                            value={formData.ageMere}
                            className="w-full px-2 py-1 bg-slate-800 border border-slate-700 rounded text-xs font-mono text-amber-400 font-bold"
                          />
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[10px] text-slate-400">Profession</label>
                          <input
                            type="text"
                            value={formData.professionMere}
                            onChange={(e) => setFormData({ ...formData, professionMere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] text-slate-400">Domicile</label>
                          <input
                            type="text"
                            value={formData.domicileMere}
                            onChange={(e) => setFormData({ ...formData, domicileMere: e.target.value })}
                            className="w-full px-2 py-1 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* SECTION 4 : DÉCLARANT & OBSERVATIONS */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <FileCheck2 className="w-4 h-4 text-amber-500" />
                    4. Déclaration, Témoins & Mentions Marginales
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Nom du déclarant *</label>
                      <input
                        type="text"
                        required
                        placeholder="Nom complet du déclarant"
                        value={formData.nomDeclarant}
                        onChange={(e) => setFormData({ ...formData, nomDeclarant: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Qualité du déclarant</label>
                      <select
                        value={formData.qualiteDeclarant}
                        onChange={(e) => setFormData({ ...formData, qualiteDeclarant: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      >
                        <option value="Père">Père</option>
                        <option value="Mère">Mère</option>
                        <option value="Sage-femme">Sage-femme</option>
                        <option value="Médecin">Médecin</option>
                        <option value="Tuteur">Tuteur</option>
                        <option value="Grand-parent">Grand-parent</option>
                        <option value="Autre">Autre</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Témoins instrumentaires</label>
                      <input
                        type="text"
                        value={formData.temoin}
                        onChange={(e) => setFormData({ ...formData, temoin: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>

                    <div className="sm:col-span-3">
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Observations / Mentions marginales</label>
                      <input
                        type="text"
                        value={formData.observations}
                        onChange={(e) => setFormData({ ...formData, observations: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                  </div>
                </div>

                {/* SECTION 5 : VALIDATION COMMUNALE & DOCUMENT DÉLIVRÉ */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-amber-500" />
                    5. Validation Communale & Document Délivré (Tables enregistrements_commune & documents)
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Officier de l'État Civil</label>
                      <input
                        type="text"
                        value={formData.nomOfficier}
                        onChange={(e) => setFormData({ ...formData, nomOfficier: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Fonction</label>
                      <input
                        type="text"
                        value={formData.fonctionOfficier}
                        onChange={(e) => setFormData({ ...formData, fonctionOfficier: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Statut de validation</label>
                      <select
                        value={formData.statutValidation}
                        onChange={(e) => setFormData({ ...formData, statutValidation: e.target.value as any })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      >
                        <option value="VALIDE">VALIDE</option>
                        <option value="EN ATTENTE">EN ATTENTE</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Type de document</label>
                      <select
                        value={formData.typeDocument}
                        onChange={(e) => setFormData({ ...formData, typeDocument: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      >
                        <option value="Extrait d'acte">Extrait d'acte</option>
                        <option value="Acte de naissance">Acte de naissance</option>
                        <option value="Copie">Copie</option>
                        <option value="Autre">Autre</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">N° du document délivré</label>
                      <input
                        type="text"
                        value={formData.numeroDocument}
                        onChange={(e) => setFormData({ ...formData, numeroDocument: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs font-mono text-slate-100"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-medium text-slate-300 mb-1">Personne ayant réceptionné</label>
                      <input
                        type="text"
                        placeholder="Nom du récepteur"
                        value={formData.personneReception}
                        onChange={(e) => setFormData({ ...formData, personneReception: e.target.value })}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                  </div>
                </div>

                {/* BOUTONS D'ACTION DU FORMULAIRE */}
                <div className="flex items-center justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      reinitialiserFormulaire();
                      setStatusMessage('Formulaire réinitialisé.');
                    }}
                    className="px-4 py-2 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
                  >
                    Effacer / Réinitialiser
                  </button>

                  <button
                    type="submit"
                    className="px-6 py-2 rounded bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-2 shadow-md transition-colors"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    Enregistrer dans la base locale SQLite
                  </button>
                </div>
              </form>
            )}

            {/* ================================================================= */}
            {/* 3. VUE : REGISTRE GÉNÉRAL (LISTE COMPLÈTE) */}
            {/* ================================================================= */}
            {currentView === 'liste' && (
              <div id="view-liste-container" className="space-y-4 max-w-6xl mx-auto">
                {/* Barre d'outils et recherche instantanée */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-3 flex-1 min-w-[280px]">
                    <div className="relative flex-1">
                      <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        type="text"
                        placeholder="Recherche rapide (Identifiant unique, Registre, Nom, Filiation...)"
                        value={filtreRapide}
                        onChange={(e) => setFiltreRapide(e.target.value)}
                        className="w-full pl-9 pr-3 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                      />
                    </div>

                    <div className="flex items-center gap-1 bg-slate-900 border border-slate-700 rounded p-1 text-xs">
                      <button
                        onClick={() => setFiltreSexe('TOUS')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                          filtreSexe === 'TOUS' ? 'bg-amber-600 text-white' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        Tous
                      </button>
                      <button
                        onClick={() => setFiltreSexe('M')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                          filtreSexe === 'M' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        Garçons (M)
                      </button>
                      <button
                        onClick={() => setFiltreSexe('F')}
                        className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                          filtreSexe === 'F' ? 'bg-rose-600 text-white' : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        Filles (F)
                      </button>
                    </div>
                  </div>

                  <div className="text-xs text-slate-400 font-mono">
                    <span className="font-bold text-amber-400">{dossiersAffiches.length}</span> dossier(s) affiché(s)
                  </div>
                </div>

                {/* TABLEAU DES DOSSIERS */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg overflow-hidden shadow-sm">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="bg-slate-900/80 border-b border-slate-800 text-slate-300 font-semibold">
                          <th className="py-3 px-3">Identifiant unique communal</th>
                          <th className="py-3 px-3">N° Registre</th>
                          <th className="py-3 px-3">Nom & Prénoms Enfant</th>
                          <th className="py-3 px-2 text-center">Sexe</th>
                          <th className="py-3 px-3">Né(e) le</th>
                          <th className="py-3 px-3">Lieu</th>
                          <th className="py-3 px-3">Filiation (Père & Mère)</th>
                          <th className="py-3 px-3 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/60 font-sans">
                        {dossiersAffiches.map((d) => (
                          <tr
                            key={d.id}
                            onClick={() => setDossierSelectionne(d)}
                            className={`cursor-pointer transition-colors ${
                              dossierSelectionne?.id === d.id ? 'bg-slate-800/80' : 'hover:bg-slate-900/40'
                            }`}
                          >
                            <td className="py-2.5 px-3 font-mono font-medium text-amber-400/90">{d.identifiantUnique}</td>
                            <td className="py-2.5 px-3 font-mono text-slate-300">{d.numeroRegistre}</td>
                            <td className="py-2.5 px-3 font-semibold text-slate-100">
                              {d.nomEnfant} {d.prenomsEnfant}
                            </td>
                            <td className="py-2.5 px-2 text-center">
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  d.sexe === 'M' ? 'bg-blue-950 text-blue-400' : 'bg-rose-950 text-rose-400'
                                }`}
                              >
                                {d.sexe}
                              </span>
                            </td>
                            <td className="py-2.5 px-3 text-slate-300">{d.dateNaissance}</td>
                            <td className="py-2.5 px-3 text-slate-400 truncate max-w-[140px]">{d.lieuNaissance}</td>
                            <td className="py-2.5 px-3 text-slate-400 text-[11px] truncate max-w-[180px]">
                              {d.nomPere ? `${d.prenomPere} ${d.nomPere} & ` : ''}
                              {d.prenomMere} {d.nomMere}
                            </td>
                            <td className="py-2.5 px-3 text-right">
                              <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                                <button
                                  title="Consulter la fiche complète"
                                  onClick={() => {
                                    setDossierSelectionne(d);
                                    setCurrentView('fiche');
                                  }}
                                  className="p-1.5 rounded hover:bg-slate-800 text-slate-400 hover:text-amber-400 transition-colors"
                                >
                                  <Eye className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  title="Imprimer l'extrait officiel"
                                  onClick={() => {
                                    setDossierAImprimer(d);
                                    setShowPrintModal(true);
                                  }}
                                  className="p-1.5 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
                                >
                                  <Printer className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  title="Supprimer"
                                  onClick={() => handleSupprimer(d.id)}
                                  className="p-1.5 rounded hover:bg-rose-950/60 text-slate-400 hover:text-rose-400 transition-colors"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* ================================================================= */}
            {/* 4. VUE : RECHERCHE MULTICRITÈRES */}
            {/* ================================================================= */}
            {currentView === 'recherche' && (
              <div id="view-recherche-container" className="space-y-6 max-w-5xl mx-auto">
                {/* Formulaire de recherche */}
                <form onSubmit={handleRecherche} className="bg-slate-950 border border-slate-800 rounded-lg p-5">
                  <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-4 pb-2 border-b border-slate-800 flex items-center gap-2">
                    <Search className="w-4 h-4 text-amber-500" />
                    Critères de Recherche d'un Acte de Naissance
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Identifiant unique communal</label>
                      <input
                        type="text"
                        placeholder="CIV-COMM-..."
                        value={searchCrit.identifiant}
                        onChange={(e) => setSearchCrit({ ...searchCrit, identifiant: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Numéro au registre</label>
                      <input
                        type="text"
                        placeholder="REG-..."
                        value={searchCrit.numRegistre}
                        onChange={(e) => setSearchCrit({ ...searchCrit, numRegistre: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Nom de l'enfant</label>
                      <input
                        type="text"
                        placeholder="Nom de famille..."
                        value={searchCrit.nomEnfant}
                        onChange={(e) => setSearchCrit({ ...searchCrit, nomEnfant: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Prénoms de l'enfant</label>
                      <input
                        type="text"
                        placeholder="Prénoms..."
                        value={searchCrit.prenomsEnfant}
                        onChange={(e) => setSearchCrit({ ...searchCrit, prenomsEnfant: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Nom du père</label>
                      <input
                        type="text"
                        placeholder="Nom du père..."
                        value={searchCrit.nomPere}
                        onChange={(e) => setSearchCrit({ ...searchCrit, nomPere: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Nom de la mère</label>
                      <input
                        type="text"
                        placeholder="Nom de la mère..."
                        value={searchCrit.nomMere}
                        onChange={(e) => setSearchCrit({ ...searchCrit, nomMere: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Année de naissance</label>
                      <input
                        type="text"
                        placeholder="Ex: 2025"
                        value={searchCrit.anneeNaissance}
                        onChange={(e) => setSearchCrit({ ...searchCrit, anneeNaissance: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Année de déclaration</label>
                      <input
                        type="text"
                        placeholder="Ex: 2025"
                        value={searchCrit.anneeDeclaration}
                        onChange={(e) => setSearchCrit({ ...searchCrit, anneeDeclaration: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded text-xs text-slate-100"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-3 mt-4 pt-3 border-t border-slate-800">
                    <button
                      type="button"
                      onClick={handleResetRecherche}
                      className="px-3 py-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium"
                    >
                      Réinitialiser les critères
                    </button>
                    <button
                      type="submit"
                      className="px-5 py-1.5 rounded bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-2"
                    >
                      <Search className="w-3.5 h-3.5" />
                      Lancer la recherche multicritères
                    </button>
                  </div>
                </form>

                {/* Résultats de la recherche */}
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="text-xs font-bold text-slate-200 uppercase tracking-wider">
                      Résultats de recherche ({resultatsRecherche.length})
                    </h4>
                  </div>

                  {resultatsRecherche.length === 0 ? (
                    <div className="py-8 text-center text-slate-500 text-xs">
                      Aucun dossier ne correspond aux critères de recherche spécifiés.
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead>
                          <tr className="border-b border-slate-800 text-slate-400 font-semibold">
                            <th className="py-2 px-2">Identifiant communal</th>
                            <th className="py-2 px-2">N° Registre</th>
                            <th className="py-2 px-2">Nom & Prénoms</th>
                            <th className="py-2 px-2">Né(e) le</th>
                            <th className="py-2 px-2">Père</th>
                            <th className="py-2 px-2">Mère</th>
                            <th className="py-2 px-2 text-right">Action</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 font-sans">
                          {resultatsRecherche.map((d) => (
                            <tr key={d.id} className="hover:bg-slate-900/50">
                              <td className="py-2 px-2 font-mono text-amber-400">{d.identifiantUnique}</td>
                              <td className="py-2 px-2 font-mono text-slate-300">{d.numeroRegistre}</td>
                              <td className="py-2 px-2 font-medium text-slate-100">
                                {d.nomEnfant} {d.prenomsEnfant}
                              </td>
                              <td className="py-2 px-2 text-slate-300">{d.dateNaissance}</td>
                              <td className="py-2 px-2 text-slate-400">{d.nomPere ? `${d.prenomPere} ${d.nomPere}` : '—'}</td>
                              <td className="py-2 px-2 text-slate-400">{d.prenomMere} {d.nomMere}</td>
                              <td className="py-2 px-2 text-right">
                                <button
                                  onClick={() => {
                                    setDossierSelectionne(d);
                                    setCurrentView('fiche');
                                  }}
                                  className="text-xs text-amber-500 hover:text-amber-400 font-semibold inline-flex items-center gap-1"
                                >
                                  <Eye className="w-3.5 h-3.5" /> Fiche
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ================================================================= */}
            {/* 5. VUE : FICHE COMPLÈTE DE L'ENFANT (EXTRAIT OFFICIEL DE NAISSANCE) */}
            {/* ================================================================= */}
            {currentView === 'fiche' && dossierSelectionne && (
              <div id="view-fiche-container" className="max-w-4xl mx-auto space-y-4">
                {/* Actions de la fiche */}
                <div className="flex items-center justify-between bg-slate-950 p-3 rounded-lg border border-slate-800">
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400">Identifiant unique communal :</span>
                    <span className="font-mono text-xs font-bold text-amber-400 bg-slate-900 px-2.5 py-1 rounded border border-amber-600/30">
                      {dossierSelectionne.identifiantUnique}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => {
                        setDossierAImprimer(dossierSelectionne);
                        setShowPrintModal(true);
                      }}
                      className="px-4 py-1.5 rounded bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-1.5 transition-colors shadow-sm"
                    >
                      <Printer className="w-3.5 h-3.5" />
                      Imprimer l'Extrait Officiel
                    </button>
                  </div>
                </div>

                {/* FICHE STYLE PARCHEMIN ADMINISTRATIF SOLENNEL */}
                <div className="bg-slate-950 border-2 border-amber-600/30 rounded-xl p-8 shadow-2xl relative">
                  {/* Sceau communal en filigrane discret */}
                  <div className="absolute right-8 top-8 w-24 h-24 rounded-full border-2 border-amber-500/20 flex flex-col items-center justify-center text-amber-500/30 select-none pointer-events-none">
                    <Building2 className="w-10 h-10 mb-1" />
                    <span className="text-[8px] font-bold uppercase tracking-wider text-center">ÉTAT CIVIL<br/>COMMUNAL</span>
                  </div>

                  {/* En-tête officiel */}
                  <div className="text-center pb-6 border-b border-slate-800/80 mb-6">
                    <p className="text-xs font-bold text-slate-400 tracking-widest uppercase">
                      RÉPUBLIQUE — SERVICE DE L'ÉTAT CIVIL
                    </p>
                    <h3 className="text-xl font-extrabold text-amber-400 uppercase tracking-wider mt-1">
                      ACTE DE NAISSANCE — FICHE OFFICIELLE
                    </h3>
                    <p className="text-xs text-slate-400 font-mono mt-1">
                      Registre communal de l'année {dossierSelectionne.anneeRegistre} — N° {dossierSelectionne.numeroRegistre}
                    </p>
                  </div>

                  {/* Bloc 1 : Identité Enfant */}
                  <div className="space-y-6">
                    <div>
                      <h4 className="text-xs font-bold text-amber-500/90 uppercase tracking-wider pb-1.5 border-b border-slate-800 flex items-center gap-2">
                        <User className="w-4 h-4 text-amber-500" />
                        1. Identité Complète de l'Enfant
                      </h4>
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-3 text-xs">
                        <div>
                          <span className="text-slate-500 block text-[10px]">Nom de famille</span>
                          <span className="font-bold text-slate-100 text-sm">{dossierSelectionne.nomEnfant}</span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Prénoms</span>
                          <span className="font-semibold text-slate-100 text-sm">{dossierSelectionne.prenomsEnfant}</span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Sexe de l'enfant</span>
                          <span className="font-medium text-slate-200">
                            {dossierSelectionne.sexe === 'M' ? 'Masculin (M)' : 'Féminin (F)'}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Date et heure de naissance</span>
                          <span className="font-medium text-slate-200">
                            {dossierSelectionne.dateNaissance} à {dossierSelectionne.heureNaissance || 'Non précisée'}
                          </span>
                        </div>
                        <div className="sm:col-span-2">
                          <span className="text-slate-500 block text-[10px]">Lieu de naissance</span>
                          <span className="font-medium text-slate-200">{dossierSelectionne.lieuNaissance}</span>
                        </div>
                      </div>
                    </div>

                    {/* Bloc 2 : Filiation */}
                    <div>
                      <h4 className="text-xs font-bold text-amber-500/90 uppercase tracking-wider pb-1.5 border-b border-slate-800 flex items-center gap-2">
                        <Users className="w-4 h-4 text-amber-500" />
                        2. Filiation Légale (Parents)
                      </h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3 text-xs">
                        <div className="p-3 bg-slate-900/60 rounded border border-slate-800/80">
                          <p className="text-[11px] font-bold text-blue-400 mb-1">Père de l'enfant</p>
                          <p className="font-semibold text-slate-100">
                            {dossierSelectionne.nomPere
                              ? `${dossierSelectionne.prenomPere} ${dossierSelectionne.nomPere}`
                              : 'Non mentionné'}
                          </p>
                          {dossierSelectionne.nomPere && (
                            <div className="text-[11px] text-slate-400 mt-1 space-y-0.5">
                              <p>Âge : {dossierSelectionne.agePere} ans</p>
                              <p>Profession : {dossierSelectionne.professionPere || 'Non renseignée'}</p>
                              <p>Domicile : {dossierSelectionne.domicilePere || 'Non renseigné'}</p>
                            </div>
                          )}
                        </div>

                        <div className="p-3 bg-slate-900/60 rounded border border-slate-800/80">
                          <p className="text-[11px] font-bold text-rose-400 mb-1">Mère de l'enfant</p>
                          <p className="font-semibold text-slate-100">
                            {dossierSelectionne.prenomMere} {dossierSelectionne.nomMere}
                          </p>
                          <div className="text-[11px] text-slate-400 mt-1 space-y-0.5">
                            <p>Âge : {dossierSelectionne.ageMere} ans</p>
                            <p>Profession : {dossierSelectionne.professionMere || 'Non renseignée'}</p>
                            <p>Domicile : {dossierSelectionne.domicileMere || 'Non renseigné'}</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Bloc 3 : Déclaration & Enregistrement communal */}
                    <div>
                      <h4 className="text-xs font-bold text-amber-500/90 uppercase tracking-wider pb-1.5 border-b border-slate-800 flex items-center gap-2">
                        <Award className="w-4 h-4 text-amber-500" />
                        3. Déclaration & Formalités Communales
                      </h4>
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-3 text-xs">
                        <div>
                          <span className="text-slate-500 block text-[10px]">Déclarant</span>
                          <span className="font-medium text-slate-200">
                            {dossierSelectionne.nomDeclarant} ({dossierSelectionne.qualiteDeclarant})
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Date de déclaration</span>
                          <span className="font-medium text-slate-200">
                            {dossierSelectionne.dateDeclaration} à {dossierSelectionne.heureDeclaration}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Officier de l'État Civil</span>
                          <span className="font-medium text-slate-200">{dossierSelectionne.nomOfficier}</span>
                        </div>
                        <div>
                          <span className="text-slate-500 block text-[10px]">Statut validation</span>
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                            {dossierSelectionne.statutValidation}
                          </span>
                        </div>
                        <div className="sm:col-span-2">
                          <span className="text-slate-500 block text-[10px]">Observations / Témoins</span>
                          <span className="font-medium text-slate-300">
                            {dossierSelectionne.observations || 'Néant'} — {dossierSelectionne.temoin}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Bas de page officiel */}
                  <div className="mt-8 pt-6 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-500">
                    <p>Délivré en mairie conformément aux registres originaux déposés.</p>
                    <div className="text-right">
                      <p className="font-semibold text-slate-300">L'Officier de l'État Civil Délégué</p>
                      <p className="text-[10px] text-amber-500/80 italic mt-0.5">Signature & Sceau certifiés</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* BARRE D'ÉTAT INFÉRIEURE DE L'APPLICATION */}
          <footer id="window-statusbar" className="h-7 bg-slate-950 border-t border-slate-800 flex items-center justify-between px-4 text-[11px] text-slate-400 shrink-0">
            <div className="flex items-center gap-3">
              <span className="text-emerald-400 font-medium flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                naissances.db (connecté)
              </span>
              <span className="text-slate-600">|</span>
              <span className="text-slate-300">{statusMessage}</span>
            </div>

            <div className="flex items-center gap-4 text-[10px] text-slate-500">
              <span>Système : Java 17 LTS / JavaFX 17 / SQLite 3.45</span>
              <span>Total : {dossiers.length} acte(s)</span>
            </div>
          </footer>
        </main>

        {/* TIROIR D'INSPECTION DU CODE SOURCE JAVAFX / FXML / CSS */}
        {showCodeInspector && (
          <div className="w-[420px] bg-slate-950 border-l border-slate-800 flex flex-col shrink-0 shadow-2xl">
            <div className="p-3 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
              <div className="flex items-center gap-2">
                <FileCode className="w-4 h-4 text-amber-500" />
                <span className="text-xs font-bold text-slate-200">Inspecteur de Code JavaFX</span>
              </div>
              <button
                onClick={() => setShowCodeInspector(false)}
                className="text-slate-400 hover:text-slate-200 text-xs px-2 py-1 rounded bg-slate-800"
              >
                Fermer
              </button>
            </div>

            <div className="p-2 border-b border-slate-800/80 flex flex-wrap gap-1 bg-slate-950">
              {Object.keys(codeFiles).map((fileName) => (
                <button
                  key={fileName}
                  onClick={() => setSelectedSourceFile(fileName)}
                  className={`px-2 py-1 rounded text-[11px] font-mono transition-colors ${
                    selectedSourceFile === fileName
                      ? 'bg-amber-600 text-white font-bold'
                      : 'bg-slate-900 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {fileName}
                </button>
              ))}
            </div>

            <div className="p-3 bg-slate-900/30 text-xs text-slate-400 border-b border-slate-800/80 flex items-center justify-between">
              <span className="truncate">{codeFiles[selectedSourceFile].desc}</span>
              <button
                onClick={() => handleCopy(codeFiles[selectedSourceFile].code, selectedSourceFile)}
                className="flex items-center gap-1 text-[10px] text-amber-400 hover:underline shrink-0 ml-2"
              >
                {copiedKey === selectedSourceFile ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                {copiedKey === selectedSourceFile ? 'Copié !' : 'Copier'}
              </button>
            </div>

            <div className="flex-1 overflow-auto p-3 font-mono text-[11px] leading-relaxed bg-slate-950 text-slate-300">
              <pre className="whitespace-pre-wrap">{codeFiles[selectedSourceFile].code}</pre>
            </div>
          </div>
        )}
      </div>

      {/* MODALE D'IMPRESSION D'EXTRAIT D'ACTE DE NAISSANCE */}
      {showPrintModal && dossierAImprimer && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-950 border border-amber-600/40 rounded-xl max-w-2xl w-full p-6 shadow-2xl space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2 text-amber-400 font-bold text-sm">
                <Printer className="w-4 h-4" />
                Aperçu de l'Extrait d'Acte de Naissance Certifié
              </div>
              <button
                onClick={() => setShowPrintModal(false)}
                className="text-slate-400 hover:text-slate-100 text-xs px-2.5 py-1 rounded bg-slate-800"
              >
                Fermer
              </button>
            </div>

            <div className="p-6 bg-slate-900/80 rounded-lg border border-slate-800 text-xs space-y-3 font-mono text-slate-200">
              <p className="text-center font-bold text-amber-400 text-sm uppercase">
                RÉPUBLIQUE — SERVICE DE L'ÉTAT CIVIL COMMUNAL
              </p>
              <p className="text-center text-[10px] text-slate-400">
                EXTRAIT DU REGISTRE DES ACTES DE NAISSANCE
              </p>
              <div className="pt-2 border-t border-slate-800 text-[11px] space-y-1">
                <p><strong>Identifiant unique communal :</strong> {dossierAImprimer.identifiantUnique}</p>
                <p><strong>N° au Registre :</strong> {dossierAImprimer.numeroRegistre} (Année {dossierAImprimer.anneeRegistre})</p>
                <p><strong>Date de naissance :</strong> {dossierAImprimer.dateNaissance} à {dossierAImprimer.heureNaissance || '08:00'}</p>
                <p><strong>Lieu de naissance :</strong> {dossierAImprimer.lieuNaissance}</p>
                <p><strong>Nom de l'enfant :</strong> {dossierAImprimer.nomEnfant}</p>
                <p><strong>Prénoms de l'enfant :</strong> {dossierAImprimer.prenomsEnfant}</p>
                <p><strong>Sexe :</strong> {dossierAImprimer.sexe === 'M' ? 'Masculin' : 'Féminin'}</p>
                <p><strong>Père :</strong> {dossierAImprimer.nomPere ? `${dossierAImprimer.prenomPere} ${dossierAImprimer.nomPere}` : 'Non déclaré'}</p>
                <p><strong>Mère :</strong> {dossierAImprimer.prenomMere} {dossierAImprimer.nomMere}</p>
              </div>
              <div className="pt-3 border-t border-slate-800 flex justify-between text-[10px] text-slate-400">
                <span>Délivré pour servir et valoir ce que de droit.</span>
                <span className="font-semibold text-slate-200">Pour extrait certifié conforme</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setShowPrintModal(false)}
                className="px-4 py-2 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
              >
                Annuler
              </button>
              <button
                onClick={() => {
                  window.print();
                  setShowPrintModal(false);
                }}
                className="px-5 py-2 rounded bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-2"
              >
                <Printer className="w-3.5 h-3.5" />
                Imprimer Document
              </button>
            </div>
          </div>
        </div>
      )}

      {/* BOÎTE DE DIALOGUE DE CONFIRMATION D'ENREGISTREMENT (PROMPT 4) */}
      {successModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-950 border border-emerald-500/50 rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-5">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-emerald-950/80 border border-emerald-600 flex items-center justify-center text-emerald-400 shrink-0">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wide">
                  Enregistrement réussi
                </h3>
                <p className="text-xs text-emerald-400 font-medium">
                  Le dossier de naissance a été enregistré avec succès dans la base SQLite.
                </p>
              </div>
            </div>

            <div className="p-4 bg-slate-900/80 rounded-lg border border-slate-800 text-xs space-y-2.5 font-mono">
              <div className="flex justify-between items-center py-1 border-b border-slate-800">
                <span className="text-slate-400">Identifiant unique communal :</span>
                <span className="text-amber-400 font-bold">{successModal.idUnique}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-800">
                <span className="text-slate-400">Numéro de registre :</span>
                <span className="text-slate-200 font-bold">{successModal.numRegistre}</span>
              </div>
              <div className="flex justify-between items-center py-1">
                <span className="text-slate-400">Enfant :</span>
                <span className="text-slate-100 font-semibold">
                  {successModal.nomEnfant} {successModal.prenomsEnfant}
                </span>
              </div>
              <div className="pt-2 text-[11px] text-slate-500 font-sans">
                ✓ Intégrité relationnelle validée : Enfant, Parents, Déclaration, Commune et Document mis à jour atomiquement.
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-1">
              <button
                type="button"
                onClick={() => {
                  setSuccessModal(null);
                  reinitialiserFormulaire();
                  setCurrentView('formulaire');
                }}
                className="px-4 py-2 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-1.5"
              >
                <Plus className="w-3.5 h-3.5" />
                Nouvelle déclaration
              </button>
              <button
                type="button"
                onClick={() => {
                  setDossierSelectionne(successModal.dossier);
                  setSuccessModal(null);
                  reinitialiserFormulaire();
                  setCurrentView('fiche');
                }}
                className="px-5 py-2 rounded bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-md"
              >
                <Eye className="w-3.5 h-3.5" />
                Consulter le dossier
              </button>
            </div>
          </div>
        </div>
      )}

      {/* BOÎTE DE DIALOGUE D'ERREUR DE VALIDATION / DOUBLON */}
      {validationErrorModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-950 border border-rose-500/50 rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-950/80 border border-rose-600 flex items-center justify-center text-rose-400 shrink-0">
                <AlertCircle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-100">
                  Erreur de validation du dossier
                </h3>
                <p className="text-xs text-rose-400">
                  L'enregistrement n'a pas pu être effectué.
                </p>
              </div>
            </div>

            <div className="p-3.5 bg-rose-950/30 border border-rose-900/50 rounded-lg text-xs text-rose-200">
              {validationErrorModal}
            </div>

            <div className="flex justify-end pt-1">
              <button
                type="button"
                onClick={() => setValidationErrorModal(null)}
                className="px-4 py-2 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold"
              >
                Corriger la saisie
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
