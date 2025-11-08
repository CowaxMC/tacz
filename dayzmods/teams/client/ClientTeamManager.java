package com.rustsayz.teams.client;

import com.rustsayz.teams.packets.PlayersListResponsePacket;
import com.rustsayz.teams.packets.TeamUpdatePacket;
import com.rustsayz.teams.packets.TeamMemberPositionPacket;
import com.rustsayz.teams.teams.Team;
import net.minecraft.client.Minecraft;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ClientTeamManager {
    private static ClientTeamManager instance;
    
    private UUID leader;
    private Set<UUID> members;
    private Map<UUID, String> playerNames; // Храним имена игроков
    private Map<UUID, TeamMemberPositionPacket.Position> memberPositions = new ConcurrentHashMap<>(); // Позиции игроков команды
    private List<PlayersListResponsePacket.PlayerInfo> playersList = new CopyOnWriteArrayList<>();
    private List<PlayersListResponsePacket.PlayerInfo> invitationsList = new CopyOnWriteArrayList<>();
    
    private ClientTeamManager() {
        this.playerNames = new HashMap<>();
    }
    
    public static ClientTeamManager getInstance() {
        if (instance == null) {
            instance = new ClientTeamManager();
        }
        return instance;
    }
    
    public void updateTeam(TeamUpdatePacket packet) {
        if (packet == null || packet.getLeader() == null) {
            this.leader = null;
            this.members = Collections.emptySet();
            this.playerNames = new HashMap<>();
        } else {
            this.leader = packet.getLeader();
            this.members = packet.getMembers();
            this.playerNames = packet.getPlayerNames(); // Сохраняем имена игроков
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen instanceof com.rustsayz.teams.client.gui.MyTeamScreen) {
            ((com.rustsayz.teams.client.gui.MyTeamScreen) mc.screen).refreshTeamData();
        }
    }
    
    public void updatePlayersList(List<PlayersListResponsePacket.PlayerInfo> players) {
        this.playersList = new CopyOnWriteArrayList<>(players);
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen instanceof com.rustsayz.teams.client.gui.InvitePlayersScreen) {
            ((com.rustsayz.teams.client.gui.InvitePlayersScreen) mc.screen).refreshPlayersList();
        }
    }
    
    public void updateInvitationsList(List<PlayersListResponsePacket.PlayerInfo> invitations) {
        this.invitationsList = new CopyOnWriteArrayList<>(invitations);
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen instanceof com.rustsayz.teams.client.gui.InvitationsScreen) {
            ((com.rustsayz.teams.client.gui.InvitationsScreen) mc.screen).refreshInvitationsList();
        }
    }
    
    public boolean hasTeam() {
        return leader != null && members != null && !members.isEmpty();
    }
    
    public UUID getLeader() {
        return leader;
    }
    
    public Set<UUID> getMembers() {
        return members != null ? new HashSet<>(members) : Collections.emptySet();
    }
    
    public List<PlayersListResponsePacket.PlayerInfo> getPlayersList() {
        return new ArrayList<>(playersList);
    }
    
    public List<PlayersListResponsePacket.PlayerInfo> getInvitationsList() {
        return new ArrayList<>(invitationsList);
    }
    
    public String getPlayerName(UUID playerId) {
        // Сначала пытаемся получить имя из сохраненных имен
        if (playerNames != null && playerNames.containsKey(playerId)) {
            return playerNames.get(playerId);
        }
        
        // Если имя не найдено, пытаемся получить из онлайн игроков
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            var player = mc.level.getPlayerByUUID(playerId);
            if (player != null) {
                return player.getDisplayName().getString();
            }
        }
        
        return null;
    }
    
    public Map<UUID, String> getPlayerNames() {
        return playerNames != null ? new HashMap<>(playerNames) : Collections.emptyMap();
    }
    
    public void updateMemberPositions(Map<UUID, TeamMemberPositionPacket.Position> positions) {
        this.memberPositions.clear();
        if (positions != null) {
            this.memberPositions.putAll(positions);
        }
    }
    
    public TeamMemberPositionPacket.Position getMemberPosition(UUID memberId) {
        return memberPositions.get(memberId);
    }
    
    public Map<UUID, TeamMemberPositionPacket.Position> getAllMemberPositions() {
        return new HashMap<>(memberPositions);
    }
}
