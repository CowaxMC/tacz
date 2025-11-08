package com.rustsayz.teams.teams;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rustsayz.teams.TeamsMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = TeamsMod.MODID)
public class TeamManager {
    private static TeamManager instance;
    
    private final Map<java.util.UUID, Set<java.util.UUID>> invitations = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Team> teams = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, java.util.UUID> playerToTeam = new ConcurrentHashMap<>();
    private Path dataFolder;
    
    private TeamManager() {
    }
    
    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TeamManager manager = getInstance();
        manager.dataFolder = event.getServer().getServerDirectory().toPath().resolve("teams_data");
        try {
            Files.createDirectories(manager.dataFolder);
            manager.loadData();
        } catch (IOException e) {
            System.err.println("[TeamManager] Ошибка при создании папки данных: " + e.getMessage());
        }
    }
    
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (instance != null) {
            instance.saveData();
        }
    }
    
    public boolean sendInvitation(java.util.UUID from, java.util.UUID to) {
        if (from.equals(to)) return false;
        if (playerToTeam.containsKey(to)) return false;
        
        invitations.computeIfAbsent(to, k -> ConcurrentHashMap.newKeySet()).add(from);
        saveData();
        return true;
    }
    
    public boolean acceptInvitation(java.util.UUID player, java.util.UUID inviter) {
        Set<java.util.UUID> playerInvitations = invitations.get(player);
        if (playerInvitations == null || !playerInvitations.contains(inviter)) {
            return false;
        }
        
        invitations.remove(player);
        
        java.util.UUID existingTeam = playerToTeam.get(inviter);
        Team team;
        
        if (existingTeam != null) {
            team = teams.get(existingTeam);
            if (team == null) {
                team = new Team(inviter);
                teams.put(inviter, team);
                // Добавляем лидера в историю при создании команды
                long currentTime = System.currentTimeMillis();
                team.getJoinDates().put(inviter, currentTime);
                String leaderName = "Unknown";
                // Имя будет обновлено позже через updatePlayerNames
                team.getMemberHistory().add(new Team.MemberHistoryEntry(inviter, leaderName, currentTime));
            }
        } else {
            team = new Team(inviter);
            teams.put(inviter, team);
            playerToTeam.put(inviter, inviter);
            // Добавляем лидера в историю при создании команды
            long currentTime = System.currentTimeMillis();
            team.getJoinDates().put(inviter, currentTime);
            String leaderName = "Unknown";
            // Имя будет обновлено позже через updatePlayerNames
            team.getMemberHistory().add(new Team.MemberHistoryEntry(inviter, leaderName, currentTime));
        }
        
        team.addMember(player);
        playerToTeam.put(player, team.getLeader());
        
        // Имена игроков будут обновлены при входе игроков через TeamEventsHandler
        // или при отправке TeamUpdatePacket из AcceptInvitationPacket
        
        saveData();
        return true;
    }
    
    public boolean declineInvitation(java.util.UUID player, java.util.UUID inviter) {
        Set<java.util.UUID> playerInvitations = invitations.get(player);
        if (playerInvitations != null && playerInvitations.remove(inviter)) {
            if (playerInvitations.isEmpty()) {
                invitations.remove(player);
            }
            saveData();
            return true;
        }
        return false;
    }
    
    public Set<java.util.UUID> getInvitations(java.util.UUID player) {
        return invitations.getOrDefault(player, Collections.emptySet());
    }
    
    public Team getPlayerTeam(java.util.UUID player) {
        java.util.UUID leaderId = playerToTeam.get(player);
        if (leaderId != null) {
            return teams.get(leaderId);
        }
        return null;
    }
    
    public Map<java.util.UUID, Team> getAllTeams() {
        return new HashMap<>(teams);
    }
    
    public Team findTeamByLeaderName(String leaderName, net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<java.util.UUID, Team> entry : teams.entrySet()) {
            Team team = entry.getValue();
            String teamLeaderName = team.getPlayerName(team.getLeader());
            
            // Проверяем сохраненное имя
            if (teamLeaderName != null && teamLeaderName.equalsIgnoreCase(leaderName)) {
                return team;
            }
            
            // Проверяем онлайн игроков
            if (server != null) {
                ServerPlayer leaderPlayer = server.getPlayerList().getPlayer(team.getLeader());
                if (leaderPlayer != null && leaderPlayer.getDisplayName().getString().equalsIgnoreCase(leaderName)) {
                    return team;
                }
            }
        }
        
        return null;
    }
    
    public boolean disbandTeam(java.util.UUID leader) {
        Team team = teams.get(leader);
        if (team == null || !team.getLeader().equals(leader)) {
            return false;
        }
        
        for (java.util.UUID member : team.getMembers()) {
            playerToTeam.remove(member);
        }
        teams.remove(leader);
        
        saveData();
        return true;
    }
    
    public boolean reassignLeader(java.util.UUID currentLeader, java.util.UUID newLeader) {
        Team team = teams.get(currentLeader);
        if (team == null || !team.getLeader().equals(currentLeader)) {
            return false;
        }
        
        if (!team.getMembers().contains(newLeader)) {
            return false;
        }
        
        Team newTeam = new Team(newLeader);
        // Копируем имена игроков и историю из старой команды
        Map<java.util.UUID, String> oldNames = team.getPlayerNames();
        Map<java.util.UUID, Long> oldJoinDates = team.getJoinDates();
        List<Team.MemberHistoryEntry> oldHistory = team.getMemberHistory();
        
        // Копируем историю
        newTeam.setMemberHistory(new ArrayList<>(oldHistory));
        
        // Добавляем лидера в историю, если его там нет
        long currentTime = System.currentTimeMillis();
        boolean leaderInHistory = false;
        for (Team.MemberHistoryEntry entry : newTeam.getMemberHistory()) {
            if (entry.getPlayerId().equals(newLeader) && entry.getLeaveDate() == null) {
                leaderInHistory = true;
                break;
            }
        }
        if (!leaderInHistory) {
            String leaderName = oldNames.getOrDefault(newLeader, "Unknown");
            newTeam.getMemberHistory().add(new Team.MemberHistoryEntry(newLeader, leaderName, currentTime));
        }
        newTeam.getJoinDates().put(newLeader, oldJoinDates.getOrDefault(newLeader, currentTime));
        
        for (java.util.UUID member : team.getMembers()) {
            if (!member.equals(currentLeader)) {
                newTeam.addMember(member);
                if (oldNames.containsKey(member)) {
                    newTeam.setPlayerName(member, oldNames.get(member));
                }
                // Копируем дату вступления
                if (oldJoinDates.containsKey(member)) {
                    newTeam.getJoinDates().put(member, oldJoinDates.get(member));
                }
            }
        }
        newTeam.addMember(currentLeader);
        if (oldNames.containsKey(currentLeader)) {
            newTeam.setPlayerName(currentLeader, oldNames.get(currentLeader));
        }
        // Копируем дату вступления для бывшего лидера
        if (oldJoinDates.containsKey(currentLeader)) {
            newTeam.getJoinDates().put(currentLeader, oldJoinDates.get(currentLeader));
        }
        
        java.util.UUID oldLeaderId = currentLeader;
        for (java.util.UUID member : team.getMembers()) {
            playerToTeam.put(member, newLeader);
        }
        
        teams.remove(oldLeaderId);
        teams.put(newLeader, newTeam);
        
        // Обновляем имена игроков (нужно передать сервер из контекста)
        // Имена будут обновлены при входе игроков через TeamEventsHandler
        
        saveData();
        return true;
    }
    
    // Обновляет имена игроков в команде на основе онлайн игроков
    private void updatePlayerNames(Team team, net.minecraft.server.MinecraftServer server) {
        if (team == null || server == null) return;
        
        for (java.util.UUID memberId : team.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                String playerName = player.getDisplayName().getString();
                team.setPlayerName(memberId, playerName);
                
                // Обновляем имя в истории, если оно не указано
                List<Team.MemberHistoryEntry> history = team.getMemberHistory();
                for (int i = 0; i < history.size(); i++) {
                    Team.MemberHistoryEntry entry = history.get(i);
                    if (entry.getPlayerId().equals(memberId) && 
                        (entry.getPlayerName() == null || entry.getPlayerName().isEmpty() || entry.getPlayerName().equals("Unknown"))) {
                        // Создаем новую запись с обновленным именем
                        Team.MemberHistoryEntry newEntry = new Team.MemberHistoryEntry(memberId, playerName, entry.getJoinDate());
                        if (entry.getLeaveDate() != null) {
                            newEntry.setLeaveDate(entry.getLeaveDate());
                        }
                        history.set(i, newEntry);
                        break;
                    }
                }
            }
        }
    }
    
    // Обновляет имя конкретного игрока во всех его командах
    public void updatePlayerName(java.util.UUID playerId, String name) {
        java.util.UUID teamLeaderId = playerToTeam.get(playerId);
        if (teamLeaderId != null) {
            Team team = teams.get(teamLeaderId);
            if (team != null) {
                team.setPlayerName(playerId, name);
                saveData();
            }
        }
    }
    
    // Обновляет имена всех игроков в команде на основе онлайн игроков
    public void updateTeamPlayerNames(java.util.UUID teamLeaderId, net.minecraft.server.MinecraftServer server) {
        Team team = teams.get(teamLeaderId);
        if (team != null && server != null) {
            updatePlayerNames(team, server);
            saveData();
        }
    }
    
    private void saveData() {
        if (dataFolder == null) return;
        
        try {
            Path invitationsFile = dataFolder.resolve("invitations.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(invitationsFile.toFile()))) {
                Map<String, Set<String>> serializableInvitations = new HashMap<>();
                for (Map.Entry<java.util.UUID, Set<java.util.UUID>> entry : invitations.entrySet()) {
                    serializableInvitations.put(
                        entry.getKey().toString(),
                        entry.getValue().stream().map(java.util.UUID::toString).collect(java.util.stream.Collectors.toSet())
                    );
                }
                oos.writeObject(serializableInvitations);
            }
            
            Path teamsFile = dataFolder.resolve("teams.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(teamsFile.toFile()))) {
                Map<String, Team> serializableTeams = new HashMap<>();
                for (Map.Entry<java.util.UUID, Team> entry : teams.entrySet()) {
                    serializableTeams.put(entry.getKey().toString(), entry.getValue());
                }
                oos.writeObject(serializableTeams);
                
                Map<String, String> serializablePlayerToTeam = new HashMap<>();
                for (Map.Entry<java.util.UUID, java.util.UUID> entry : playerToTeam.entrySet()) {
                    serializablePlayerToTeam.put(entry.getKey().toString(), entry.getValue().toString());
                }
                oos.writeObject(serializablePlayerToTeam);
            }
        } catch (IOException e) {
            System.err.println("[TeamManager] Ошибка при сохранении данных: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadData() {
        if (dataFolder == null) return;
        
        try {
            Path invitationsFile = dataFolder.resolve("invitations.dat");
            if (Files.exists(invitationsFile)) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(invitationsFile.toFile()))) {
                    Map<String, Set<String>> serializableInvitations = (Map<String, Set<String>>) ois.readObject();
                    invitations.clear();
                    for (Map.Entry<String, Set<String>> entry : serializableInvitations.entrySet()) {
                        java.util.UUID player = java.util.UUID.fromString(entry.getKey());
                        Set<java.util.UUID> inviters = entry.getValue().stream()
                            .map(java.util.UUID::fromString)
                            .collect(java.util.stream.Collectors.toCollection(() -> ConcurrentHashMap.newKeySet()));
                        invitations.put(player, inviters);
                    }
                }
            }
            
            Path teamsFile = dataFolder.resolve("teams.dat");
            if (Files.exists(teamsFile)) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(teamsFile.toFile()))) {
                    Map<String, Team> serializableTeams = (Map<String, Team>) ois.readObject();
                    Map<String, String> serializablePlayerToTeam = (Map<String, String>) ois.readObject();
                    
                    teams.clear();
                    for (Map.Entry<String, Team> entry : serializableTeams.entrySet()) {
                        java.util.UUID leaderId = java.util.UUID.fromString(entry.getKey());
                        teams.put(leaderId, entry.getValue());
                    }
                    
                    playerToTeam.clear();
                    for (Map.Entry<String, String> entry : serializablePlayerToTeam.entrySet()) {
                        playerToTeam.put(java.util.UUID.fromString(entry.getKey()), java.util.UUID.fromString(entry.getValue()));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[TeamManager] Ошибка при загрузке данных: " + e.getMessage());
        }
    }
}
