package com.rustsayz.teams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.rustsayz.teams.teams.TeamManager;
import com.rustsayz.teams.teams.Team;
import com.rustsayz.teams.PacketHandler;
import com.rustsayz.teams.packets.TeamUpdatePacket;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeamsCommands {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teams")
            .requires(source -> source.hasPermission(2)) // Требует уровень 2 (модератор/админ)
            .then(Commands.literal("all")
                .executes(TeamsCommands::listAllTeams))
            .then(Commands.literal("info")
                .then(Commands.argument("leader", StringArgumentType.string())
                    .executes(TeamsCommands::teamInfo)))
            .then(Commands.literal("delete")
                .then(Commands.argument("leader", StringArgumentType.string())
                    .executes(TeamsCommands::deleteTeam))));
    }
    
    private static int listAllTeams(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TeamManager manager = TeamManager.getInstance();
        
        Map<java.util.UUID, Team> teams = manager.getAllTeams();
        
        if (teams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cНет созданных команд"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Список всех команд ==="), false);
        
        for (Map.Entry<java.util.UUID, Team> entry : teams.entrySet()) {
            final Team team = entry.getValue();
            String leaderName = team.getPlayerName(team.getLeader());
            if (leaderName == null) {
                leaderName = "Неизвестно";
            }
            final String finalLeaderName = leaderName;
            final int memberCount = team.size();
            source.sendSuccess(() -> Component.literal("§e" + finalLeaderName + " §7- §a" + memberCount + " участников"), false);
        }
        
        return 1;
    }
    
    private static int teamInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String leaderName = StringArgumentType.getString(context, "leader");
        TeamManager manager = TeamManager.getInstance();
        
        // Ищем команду по имени лидера
        Team team = manager.findTeamByLeaderName(leaderName, source.getServer());
        
        if (team == null) {
            source.sendFailure(Component.literal("§cКоманда с лидером '" + leaderName + "' не найдена"));
            return 0;
        }
        
        String actualLeaderName = team.getPlayerName(team.getLeader());
        if (actualLeaderName == null) {
            actualLeaderName = "Неизвестно";
        }
        
        final String finalActualLeaderName = actualLeaderName;
        final Team finalTeam = team;
        source.sendSuccess(() -> Component.literal("§6=== Информация о команде ==="), false);
        source.sendSuccess(() -> Component.literal("§eЛидер: §a" + finalActualLeaderName), false);
        source.sendSuccess(() -> Component.literal("§eУчастников: §a" + finalTeam.size()), false);
        source.sendSuccess(() -> Component.literal("§6--- Участники команды ---"), false);
        
        for (java.util.UUID memberId : team.getMembers()) {
            String memberName = team.getPlayerName(memberId);
            if (memberName == null) {
                memberName = "Неизвестно";
            }
            
            Long joinDate = team.getJoinDate(memberId);
            String joinDateStr = "Не указано";
            if (joinDate != null) {
                joinDateStr = DATE_FORMAT.format(new Date(joinDate));
            }
            
            final String finalMemberName = memberName;
            final String finalJoinDateStr = joinDateStr;
            final boolean isLeader = memberId.equals(team.getLeader());
            source.sendSuccess(() -> Component.literal((isLeader ? "§6[Лидер] " : "§7") + finalMemberName + " §7- Дата вступления: §a" + finalJoinDateStr), false);
        }
        
        source.sendSuccess(() -> Component.literal("§6--- История участников ---"), false);
        List<Team.MemberHistoryEntry> history = team.getMemberHistory();
        
        if (history.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7История не указана"), false);
        } else {
            for (Team.MemberHistoryEntry entry : history) {
                String entryName = entry.getPlayerName();
                if (entryName == null || entryName.isEmpty()) {
                    entryName = "Неизвестно";
                }
                
                String joinDateStr = DATE_FORMAT.format(new Date(entry.getJoinDate()));
                String leaveDateStr = "В команде";
                if (entry.getLeaveDate() != null) {
                    leaveDateStr = DATE_FORMAT.format(new Date(entry.getLeaveDate()));
                }
                
                final String finalEntryName = entryName;
                final String finalJoinDateStr = joinDateStr;
                final String finalLeaveDateStr = leaveDateStr;
                source.sendSuccess(() -> Component.literal("§7" + finalEntryName + " §7- Вступил: §a" + finalJoinDateStr + " §7- Вышел: §c" + finalLeaveDateStr), false);
            }
        }
        
        return 1;
    }
    
    private static int deleteTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String leaderName = StringArgumentType.getString(context, "leader");
        TeamManager manager = TeamManager.getInstance();
        
        // Ищем команду по имени лидера
        Team team = manager.findTeamByLeaderName(leaderName, source.getServer());
        
        if (team == null) {
            source.sendFailure(Component.literal("§cКоманда с лидером '" + leaderName + "' не найдена"));
            return 0;
        }
        
        java.util.UUID leaderId = team.getLeader();
        
        // Удаляем команду
        if (manager.disbandTeam(leaderId)) {
            source.sendSuccess(() -> Component.literal("§aКоманда '" + leaderName + "' успешно удалена"), false);
            
            // Уведомляем всех участников команды
            net.minecraft.server.MinecraftServer server = source.getServer();
            if (server != null) {
                for (java.util.UUID memberId : team.getMembers()) {
                    ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        member.sendSystemMessage(Component.literal("§cВаша команда была удалена администратором"));
                        PacketHandler.sendToClient(member, new TeamUpdatePacket(null, null));
                    }
                }
            }
            
            return 1;
        } else {
            source.sendFailure(Component.literal("§cНе удалось удалить команду"));
            return 0;
        }
    }
}

