package com.gestionnaissances.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Matrice d'habilitation et vérificateur des permissions par rôle applicatif.
 * L'application des règles de sécurité est strictement appliquée au niveau du code.
 */
public class RolePermissions {

    private static final Set<Permission> PERMISSIONS_ADMIN = Collections.unmodifiableSet(
            EnumSet.allOf(Permission.class)
    );

    private static final Set<Permission> PERMISSIONS_RESPONSABLE = Collections.unmodifiableSet(
            EnumSet.of(
                    Permission.VOIR_DASHBOARD,
                    Permission.ENREGISTRER_NAISSANCE,
                    Permission.LISTER_NAISSANCES,
                    Permission.RECHERCHER_NAISSANCES,
                    Permission.CONSULTER_PARENTS,
                    Permission.VOIR_STATISTIQUES,
                    Permission.CONSULTER_REGISTRE,
                    Permission.GERER_DOCUMENTS,
                    Permission.CONSULTER_FICHES,
                    Permission.MODIFIER_DOSSIERS
                    // EXCLUS : GERER_UTILISATEURS, ADMINISTRER_UTILISATEURS, PARAMETRES_SYSTEME, SAUVEGARDE_BASE, SUPPRESSION_DEFINITIVE
            )
    );

    private static final Set<Permission> PERMISSIONS_AGENT = Collections.unmodifiableSet(
            EnumSet.of(
                    Permission.VOIR_DASHBOARD,
                    Permission.ENREGISTRER_NAISSANCE,
                    Permission.LISTER_NAISSANCES,
                    Permission.RECHERCHER_NAISSANCES,
                    Permission.CONSULTER_PARENTS,
                    Permission.VOIR_STATISTIQUES,
                    Permission.CONSULTER_REGISTRE,
                    Permission.GERER_DOCUMENTS,
                    Permission.CONSULTER_FICHES
                    // EXCLUS : MODIFIER_DOSSIERS, GERER_UTILISATEURS, ADMINISTRER_UTILISATEURS, PARAMETRES_SYSTEME, SAUVEGARDE_BASE, SUPPRESSION_DEFINITIVE
            )
    );

    /**
     * Vérifie si un rôle possède une permission spécifique.
     *
     * @param role       Le rôle de l'utilisateur
     * @param permission La permission requise
     * @return true si l'action est autorisée, false sinon.
     */
    public static boolean aPermission(Role role, Permission permission) {
        if (role == null || permission == null) {
            return false;
        }
        switch (role) {
            case ADMINISTRATEUR:
                return PERMISSIONS_ADMIN.contains(permission);
            case RESPONSABLE_COMMUNAL:
                return PERMISSIONS_RESPONSABLE.contains(permission);
            case AGENT:
                return PERMISSIONS_AGENT.contains(permission);
            default:
                return false;
        }
    }

    /**
     * Renvoie la liste complète des permissions associées à un rôle.
     */
    public static Set<Permission> getPermissions(Role role) {
        if (role == null) {
            return Collections.emptySet();
        }
        switch (role) {
            case ADMINISTRATEUR:
                return PERMISSIONS_ADMIN;
            case RESPONSABLE_COMMUNAL:
                return PERMISSIONS_RESPONSABLE;
            case AGENT:
                return PERMISSIONS_AGENT;
            default:
                return Collections.emptySet();
        }
    }
}
