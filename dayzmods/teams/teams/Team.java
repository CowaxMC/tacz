package com.rustsayz.teams.teams;

import java.io.Serializable;
import java.util.*;

public class Team implements Serializable {
    private static final long serialVersionUID = 3L;
    
    private java.util.UUID leader;
    private Set<java.util.UUID> members;
    private Map<java.util.UUID, String> playerNames; // Храним имена игроков
    private Map<java.util.UUID, Long> joinDates; // Дата вступления в команду (timestamp в миллисекундах)
    private List<MemberHistoryEntry> memberHistory; // История участников команды
    
    public static class MemberHistoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private java.util.UUID playerId;
        private String playerName;
        private long joinDate; // Дата вступления
        private Long leaveDate; // Дата выхода (null если еще в команде)
        
        public MemberHistoryEntry(java.util.UUID playerId, String playerName, long joinDate) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.joinDate = joinDate;
            this.leaveDate = null;
        }
        
        public java.util.UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public long getJoinDate() { return joinDate; }
        public Long getLeaveDate() { return leaveDate; }
        public void setLeaveDate(Long leaveDate) { this.leaveDate = leaveDate; }
    }
    
    public Team(java.util.UUID leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.playerNames = new HashMap<>();
        this.joinDates = new HashMap<>();
        this.memberHistory = new ArrayList<>();
    }
    
    public java.util.UUID getLeader() {
        return leader;
    }
    
    public void setLeader(java.util.UUID leader) {
        if (members.contains(leader)) {
            this.leader = leader;
        }
    }
    
    public Set<java.util.UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    public void addMember(java.util.UUID member) {
        members.add(member);
        long currentTime = System.currentTimeMillis();
        joinDates.put(member, currentTime);
        
        // Добавляем в историю
        String name = playerNames.getOrDefault(member, "Unknown");
        memberHistory.add(new MemberHistoryEntry(member, name, currentTime));
    }
    
    public void removeMember(java.util.UUID member) {
        members.remove(member);
        playerNames.remove(member);
        joinDates.remove(member);
        
        // Обновляем историю - помечаем дату выхода
        for (MemberHistoryEntry entry : memberHistory) {
            if (entry.getPlayerId().equals(member) && entry.getLeaveDate() == null) {
                entry.setLeaveDate(System.currentTimeMillis());
                break;
            }
        }
    }
    
    public boolean hasMember(java.util.UUID member) {
        return members.contains(member);
    }
    
    public int size() {
        return members.size();
    }
    
    public void setPlayerName(java.util.UUID player, String name) {
        if (members.contains(player)) {
            playerNames.put(player, name);
        }
    }
    
    public String getPlayerName(java.util.UUID player) {
        return playerNames.getOrDefault(player, null);
    }
    
    public Map<java.util.UUID, String> getPlayerNames() {
        return new HashMap<>(playerNames);
    }
    
    public void setPlayerNames(Map<java.util.UUID, String> names) {
        this.playerNames = new HashMap<>(names);
    }
    
    public Map<java.util.UUID, Long> getJoinDates() {
        return new HashMap<>(joinDates);
    }
    
    public Long getJoinDate(java.util.UUID member) {
        return joinDates.get(member);
    }
    
    public List<MemberHistoryEntry> getMemberHistory() {
        return new ArrayList<>(memberHistory);
    }
    
    public void setJoinDates(Map<java.util.UUID, Long> dates) {
        this.joinDates = new HashMap<>(dates);
    }
    
    public void setMemberHistory(List<MemberHistoryEntry> history) {
        this.memberHistory = new ArrayList<>(history);
    }
}
