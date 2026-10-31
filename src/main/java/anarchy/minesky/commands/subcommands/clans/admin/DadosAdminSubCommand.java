package anarchy.minesky.commands.subcommands.clans.admin;

import anarchy.minesky.AnarchyClans;
import anarchy.minesky.commands.subcommands.SubCommand;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.data.Clan;
import anarchy.minesky.data.MemberData;
import anarchy.minesky.enums.ClanRoles;
import anarchy.minesky.handlers.ClanHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static anarchy.minesky.commands.ClanCommand.sendError;

public class DadosAdminSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "dados";
    }

    @Override
    public String getDescription() {
        return "Gerenciar dados de clãs e jogadores (editar, deletar, resetar)";
    }

    @Override
    public String getUsage() {
        return "/clan admin dados <editar,deletar,resetar> <clans,jogadores> <tag,nick> [campo] [valor]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("data");
    }

    @Override
    public boolean getAdminCommand() {
        return true;
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            sendError(player, "&cUso: " + getUsage());
            return;
        }

        String action = args[1].toLowerCase();
        String targetType = args[2].toLowerCase();
        String targetName = args[3];

        if (targetType.equals("clans") || targetType.equals("guildas")) {
            handleClans(player, action, targetName, args);
        } else if (targetType.equals("jogadores") || targetType.equals("players")) {
            handleJogadores(player, action, targetName, args);
        } else {
            sendError(player, "&cTipo de alvo inválido. Use 'clans' ou 'jogadores'.");
        }
    }

    private void handleClans(Player admin, String action, String tag, String[] args) {
        Clan clan = ClanHandler.getClanByTag(tag);
        if (clan == null) {
            sendError(admin, "&cClã com a tag '" + tag + "' não foi encontrado.");
            return;
        }

        switch (action) {
            case "deletar" -> {
                AnarchyClans.l.info("[ADMIN] " + admin.getName() + " deletou o clã " + clan.getName());
                ClanHandler.deleteClan(clan.getId());
                FeedbackAPI.display(admin, "&aClã &f" + clan.getName() + " &adeletado com sucesso.");
            }

            case "resetar" -> {
                clan.setLevel(0);
                clan.setXP(0.0);
                clan.setBalance(0);
                clan.setBase(null);
                ClanHandler.saveClan(clan);

                AnarchyClans.l.info("[ADMIN] " + admin.getName() + " resetou os dados do clã " + clan.getName());
                FeedbackAPI.display(admin, "&aDados do clã &f" + clan.getName() + " &aresetados para o nível 0.");
            }

            case "editar" -> {
                if (args.length < 6) {
                    sendError(admin, "&cUso: /clan admin dados editar clans <tag> <level,xp,balance,mudar-lider> <valor>");
                    return;
                }

                String field = args[4].toLowerCase();
                String valueStr = args[5];

                if (field.equals("mudar-lider")) {
                    handleMudarLiderField(admin, clan, valueStr);
                    return;
                }

                try {
                    switch (field) {
                        case "level" -> {
                            int level = Integer.parseInt(valueStr);
                            clan.setLevel(Math.max(0, level));
                            ClanHandler.saveClan(clan);
                            AnarchyClans.l.info("[ADMIN] " + admin.getName() + " definiu o nível do clã " + clan.getName() + " para " + level);
                            FeedbackAPI.display(admin, "&aNível do clã &f" + clan.getName() + " &adefinido para &f" + level + "&a.");
                        }
                        case "xp" -> {
                            double xp = Double.parseDouble(valueStr);
                            clan.setXP(Math.max(0.0, xp));
                            ClanHandler.saveClan(clan);
                            AnarchyClans.l.info("[ADMIN] " + admin.getName() + " definiu o XP do clã " + clan.getName() + " para " + xp);
                            FeedbackAPI.display(admin, "&aXP do clã &f" + clan.getName() + " &adefinido para &f" + xp + "&a.");
                        }
                        case "balance" -> {
                            int balance = Integer.parseInt(valueStr);
                            clan.setBalance(Math.max(0, balance));
                            ClanHandler.saveClan(clan);
                            AnarchyClans.l.info("[ADMIN] " + admin.getName() + " definiu o saldo do clã " + clan.getName() + " para $" + balance);
                            FeedbackAPI.display(admin, "&aSaldo do clã &f" + clan.getName() + " &adefinido para &f$" + balance + "&a.");
                        }
                        default -> sendError(admin, "&cCampo inválido. Use 'level', 'xp', 'balance' ou 'mudar-lider'.");
                    }
                } catch (NumberFormatException e) {
                    sendError(admin, "&cO valor inserido deve ser numérico.");
                }
            }

            default -> sendError(admin, "&cAção inválida. Opções: 'deletar', 'editar' ou 'resetar'.");
        }
    }

    private void handleMudarLiderField(Player admin, Clan clan, String newLeaderNick) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(newLeaderNick);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(admin, "&cJogador '" + newLeaderNick + "' não encontrado.");
            return;
        }

        UUID newLeaderUuid = target.getUniqueId();
        UUID oldLeaderUuid = clan.getLeader();

        if (newLeaderUuid.equals(oldLeaderUuid)) {
            sendError(admin, "&cEste jogador já é o líder do clã.");
            return;
        }

        Clan otherClan = ClanHandler.getClanByPlayer(newLeaderUuid);
        if (otherClan != null && !otherClan.getId().equals(clan.getId())) {
            otherClan.removeMember(newLeaderUuid);
            ClanHandler.saveClan(otherClan);
            admin.sendMessage("§7" + target.getName() + " foi removido do clã " + otherClan.getName() + ".");
        }

        if (!clan.getMembers().containsKey(newLeaderUuid)) {
            clan.addMember(newLeaderUuid, ClanRoles.LEADER, 0, System.currentTimeMillis());
        } else {
            clan.getMemberData(newLeaderUuid).setRole(ClanRoles.LEADER);
        }

        MemberData oldLeaderData = clan.getMemberData(oldLeaderUuid);
        if (oldLeaderData != null) {
            oldLeaderData.setRole(ClanRoles.SUB_LEADER);
        }

        clan.setLeader(newLeaderUuid);
        ClanHandler.saveClan(clan);

        AnarchyClans.l.info("[ADMIN] " + admin.getName() + " transferiu a liderança do clã " + clan.getName() + " para " + target.getName());
        FeedbackAPI.display(admin, "&aLiderança de &f" + clan.getName() + " &atransferida para &f" + target.getName() + "&a.");

        Player newLeaderOnline = target.getPlayer();
        if (newLeaderOnline != null && newLeaderOnline.isOnline()) {
            FeedbackAPI.display(newLeaderOnline, "&aVocê foi definido como líder do clã &f" + clan.getName() + "&a por um admin.");
        }
    }

    private void handleJogadores(Player admin, String action, String nick, String[] args) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendError(admin, "&cJogador '" + nick + "' não encontrado.");
            return;
        }

        UUID uuid = target.getUniqueId();
        Clan clan = ClanHandler.getClanByPlayer(uuid);

        switch (action) {
            case "deletar" -> {
                if (clan == null) {
                    sendError(admin, "&cEste jogador não pertence a nenhum clã.");
                    return;
                }
                clan.removeMember(uuid);
                ClanHandler.saveClan(clan);

                AnarchyClans.l.info("[ADMIN] " + admin.getName() + " removeu " + target.getName() + " do clã " + clan.getName());
                FeedbackAPI.display(admin, "&aJogador &f" + target.getName() + " &aremovido do clã &f" + clan.getName() + "&a.");
            }

            case "resetar" -> {
                if (clan != null) {
                    MemberData data = clan.getMemberData(uuid);
                    if (data != null) {
                        data.setKills(0);
                    }
                    ClanHandler.saveClan(clan);
                }
                AnarchyClans.l.info("[ADMIN] " + admin.getName() + " resetou os dados do jogador " + target.getName());
                FeedbackAPI.display(admin, "&aEstatísticas do jogador &f" + target.getName() + " &aresetadas.");
            }

            case "editar" -> {
                if (args.length < 6) {
                    sendError(admin, "&cUso: /clan admin dados editar jogadores <nick> <kills,role> <valor>");
                    return;
                }

                if (clan == null) {
                    sendError(admin, "&cEste jogador não pertence a um clã para ser editado.");
                    return;
                }

                MemberData data = clan.getMemberData(uuid);
                if (data == null) {
                    sendError(admin, "&cMetadados do jogador não encontrados no clã.");
                    return;
                }

                String field = args[4].toLowerCase();
                String valueStr = args[5];

                if (field.equals("kills")) {
                    try {
                        int kills = Integer.parseInt(valueStr);
                        data.setKills(Math.max(0, kills));
                        ClanHandler.saveClan(clan);

                        AnarchyClans.l.info("[ADMIN] " + admin.getName() + " definiu as kills de " + target.getName() + " para " + kills);
                        FeedbackAPI.display(admin, "&aKills de &f" + target.getName() + " &adefinidas para &f" + kills + "&a.");
                    } catch (NumberFormatException e) {
                        sendError(admin, "&cO valor de kills deve ser um número inteiro.");
                    }
                } else if (field.equals("role")) {
                    ClanRoles role = ClanRoles.getRole(valueStr);
                    if (role == null) {
                        sendError(admin, "&cCargo inválido. Opções: Líder, Sub-Líder, Capitão, Recrutador, Leal, Membro, Recruta.");
                        return;
                    }

                    data.setRole(role);
                    ClanHandler.saveClan(clan);

                    AnarchyClans.l.info("[ADMIN] " + admin.getName() + " definiu o cargo de " + target.getName() + " para " + role.getLabel());
                    FeedbackAPI.display(admin, "&aCargo de &f" + target.getName() + " &adefinido para &f" + role.getLabel() + "&a.");
                } else {
                    sendError(admin, "&cCampo inválido. Escolha 'kills' ou 'role'.");
                }
            }

            default -> sendError(admin, "&cAção inválida. Opções: 'deletar', 'editar' ou 'resetar'.");
        }
    }
}