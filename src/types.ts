export interface DossierComplet {
  id: number;
  identifiantUnique: string; // Ex: CIV-COMM-2025-00001
  numeroRegistre: string;    // Ex: REG-2025-0001
  anneeRegistre: number;
  folio: string;
  tome: string;
  dateDeclaration: string;
  heureDeclaration: string;
  
  // Enfant
  nomEnfant: string;
  prenomsEnfant: string;
  sexe: 'M' | 'F';
  statutEnfant: string;
  dateNaissance: string;
  heureNaissance: string;
  lieuNaissance: string;

  // Père
  nomPere: string;
  prenomPere: string;
  dateNaissancePere?: string;
  agePere: number;
  professionPere: string;
  domicilePere: string;

  // Mère
  nomMere: string;
  prenomMere: string;
  dateNaissanceMere?: string;
  ageMere: number;
  professionMere: string;
  domicileMere: string;

  // Déclaration
  nomDeclarant: string;
  qualiteDeclarant: string;
  temoin: string;
  observations: string;

  // Commune & Validation
  nomOfficier: string;
  fonctionOfficier: string;
  statutValidation: 'VALIDE' | 'EN ATTENTE' | 'RECTIFIE';

  // Document officiel
  typeDocument: string;
  numeroDocument: string;
  dateDelivrance: string;
  heureDelivrance: string;
  personneReception: string;
  confirmationLivraison: boolean;
}

export type NaissanceRecord = DossierComplet;
