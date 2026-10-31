package anarchy.minesky.enums;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public enum ClanRoles {
    LEADER("Líder"),               // Acesso total (Ordinal 0)
    SUB_LEADER("Sub-Líder"),       // Gerencia membros, guerras e banco (Ordinal 1)
    CAPTAIN("Capitão"),            // Recruta, expulsa membros e chat líderes (Ordinal 2)
    RECRUITER("Recrutador"),       // Apenas recruta (Ordinal 3)
    LOYAL("Leal"),                 // Membro experiente (Ordinal 4)
    MEMBER("Membro"),              // Membro padrão (Ordinal 5)
    RECRUIT("Recruta");            // Membro recém-adicionado (Ordinal 6)

    private static final ClanRoles[] VALUES = values();
    private final String label;

    ClanRoles(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Retorna true se este cargo tiver maior autoridade que o outro (menor ordinal).
     */
    public boolean isHigherThan(ClanRoles other) {
        return other != null && this.ordinal() < other.ordinal();
    }

    /**
     * Retorna true se este cargo for inferior ao outro (maior ordinal).
     */
    public boolean isLowerThan(ClanRoles other) {
        return other != null && this.ordinal() > other.ordinal();
    }

    /**
     * Verifica se o executor pode promover o jogador alvo para o cargo desejado.
     * (O executor deve ser superior ao cargo desejado, e o cargo desejado superior ao atual).
     */
    public static boolean canPromoteTo(ClanRoles executorRole, ClanRoles targetRole, ClanRoles desiredRole) {
        if (executorRole == null || targetRole == null || desiredRole == null) return false;
        return executorRole.isHigherThan(desiredRole) && desiredRole.isHigherThan(targetRole);
    }

    public static boolean canPermission(ClanRoles executorRole, ClanRoles targetRole) {
        return executorRole != null && executorRole.isHigherThan(targetRole);
    }

    /**
     * Retorna todos os cargos acima do cargo atual.
     */
    public static List<ClanRoles> getRolesAbove(ClanRoles currentRole) {
        if (currentRole == null) return List.of();
        List<ClanRoles> roles = new ArrayList<>();
        for (ClanRoles role : VALUES) {
            if (role.isHigherThan(currentRole)) roles.add(role);
        }
        return roles;
    }

    /**
     * Retorna os cargos que o jogador pode promover alguém.
     */
    public static List<ClanRoles> getPromotableRoles(ClanRoles playerRole) {
        if (playerRole == null) return List.of();
        List<ClanRoles> roles = new ArrayList<>();
        for (ClanRoles role : VALUES) {
            if (role.isLowerThan(playerRole)) roles.add(role);
        }
        return roles;
    }

    public boolean isLeadership() {
        return this.ordinal() <= CAPTAIN.ordinal(); // LEADER, SUB_LEADER, CAPTAIN
    }

    public boolean isLeaders() {
        return this.ordinal() <= SUB_LEADER.ordinal(); // LEADER, SUB_LEADER
    }

    public boolean isLeadershipAndRecruiter() {
        return this.ordinal() <= RECRUITER.ordinal(); // LEADER, SUB_LEADER, CAPTAIN, RECRUITER
    }

    public static boolean isLeadership(ClanRoles role) {
        return role != null && role.isLeadership();
    }

    public static boolean isLeaders(ClanRoles role) {
        return role != null && role.isLeaders();
    }

    public static boolean isLeadershipAndRecruiter(ClanRoles role) {
        return role != null && role.isLeadershipAndRecruiter();
    }

    public static ClanRoles getRole(String name) {
        if (name == null || name.isBlank()) return null;

        String clean = Normalizer.normalize(name.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("-", "")
                .replace("_", "");

        return switch (clean) {
            case "leader", "lider" -> LEADER;
            case "subleader", "sublider" -> SUB_LEADER;
            case "captain", "capitao" -> CAPTAIN;
            case "recruiter", "recrutador" -> RECRUITER;
            case "loyal", "leal" -> LOYAL;
            case "member", "membro" -> MEMBER;
            case "recruit", "recruta" -> RECRUIT;
            default -> null;
        };
    }

    public static String getLabelRole(ClanRoles role) {
        return role != null ? role.getLabel() : "";
    }
}