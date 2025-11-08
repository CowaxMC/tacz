package com.rustsayz.teams.teams;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rustsayz.teams.TeamsMod;
import com.rustsayz.teams.PacketHandler;
import com.rustsayz.teams.packets.TeamUpdatePacket;
import com.rustsayz.teams.packets.TeamMemberPositionPacket;
import com.rustsayz.teams.effects.ModEffects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = TeamsMod.MODID)
public class TeamEventsHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamManager manager = TeamManager.getInstance();
            
            // Обновляем имя игрока в его команде
            manager.updatePlayerName(player.getUUID(), player.getDisplayName().getString());
            
            // Обновляем имена всех игроков в команде игрока
            var team = manager.getPlayerTeam(player.getUUID());
            if (team != null) {
                // Обновляем имя текущего игрока
                team.setPlayerName(player.getUUID(), player.getDisplayName().getString());
                
                // Обновляем имена всех игроков в команде на основе онлайн игроков
                manager.updateTeamPlayerNames(team.getLeader(), player.getServer());
                
                // Отправляем обновленную информацию о команде всем участникам
                TeamUpdatePacket packet = new TeamUpdatePacket(team);
                PacketHandler.sendToClient(player, packet);
                
                // Отправляем обновление другим участникам команды
                for (java.util.UUID memberId : team.getMembers()) {
                    if (!memberId.equals(player.getUUID())) {
                        ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                        if (member != null) {
                            PacketHandler.sendToClient(member, packet);
                        }
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Игрок вышел - ничего не делаем с скинами
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        // Проверяем, что цель - игрок
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }
        
        // Получаем источник урона
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        
        // Проверяем, что урон нанесен игроком
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        
        // Не проверяем урон самому себе
        if (attacker.equals(target)) {
            return;
        }
        
        // Проверяем, находятся ли игроки в одной команде
        TeamManager manager = TeamManager.getInstance();
        Team attackerTeam = manager.getPlayerTeam(attacker.getUUID());
        Team targetTeam = manager.getPlayerTeam(target.getUUID());
        
        // Если оба игрока в командах и это одна и та же команда - отменяем урон
        if (attackerTeam != null && targetTeam != null && 
            attackerTeam.getLeader().equals(targetTeam.getLeader())) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        // Проверяем, что атакующий - игрок
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        
        // Проверяем, что цель - игрок
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        
        // Не проверяем атаку самого себя
        if (attacker.equals(target)) {
            return;
        }
        
        // Проверяем, находятся ли игроки в одной команде
        TeamManager manager = TeamManager.getInstance();
        Team attackerTeam = manager.getPlayerTeam(attacker.getUUID());
        Team targetTeam = manager.getPlayerTeam(target.getUUID());
        
        // Если оба игрока в командах и это одна и та же команда - отменяем атаку
        if (attackerTeam != null && targetTeam != null && 
            attackerTeam.getLeader().equals(targetTeam.getLeader())) {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Отправляем позиции игроков команды каждые 10 тиков (0.5 секунды)
        TeamManager manager = TeamManager.getInstance();
        net.minecraft.server.MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        
        // Для каждого игрока отправляем позиции его тиммейтов
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = manager.getPlayerTeam(player.getUUID());
            if (team != null) {
                Map<java.util.UUID, TeamMemberPositionPacket.Position> positions = new HashMap<>();
                
                Set<java.util.UUID> members = team.getMembers();
                for (java.util.UUID memberId : members) {
                    if (memberId.equals(player.getUUID())) {
                        continue; // Пропускаем самого игрока
                    }
                    
                    ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        // Добавляем позицию онлайн игрока
                        positions.put(memberId, new TeamMemberPositionPacket.Position(
                            member.getX(),
                            member.getY(),
                            member.getZ()
                        ));
                    }
                }
                
                // Отправляем пакет с позициями
                if (!positions.isEmpty()) {
                    TeamMemberPositionPacket packet = new TeamMemberPositionPacket(positions);
                    PacketHandler.sendToClient(player, packet);
                }
            }
        }
    }
}
