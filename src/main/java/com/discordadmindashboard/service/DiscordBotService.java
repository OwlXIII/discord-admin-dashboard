package com.discordadmindashboard.service;

import com.discordadmindashboard.config.DiscordProperties;
import com.discordadmindashboard.dto.MemberDto;
import com.discordadmindashboard.dto.RoleDto;
import com.discordadmindashboard.exception.BotUnavailableException;
import com.discordadmindashboard.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Wraps the JDA gateway connection used for privileged, server-side actions
 * (listing members, managing roles, kicking). Unlike the original, the bot is
 * <em>optional</em>: if no token is configured the application still starts and
 * bot-backed endpoints respond with 503 instead of crashing on boot.
 */
@Service
public class DiscordBotService {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotService.class);
    private static final long MEMBER_LOAD_TIMEOUT_SECONDS = 30;

    private final DiscordProperties config;
    private volatile JDA jda;

    public DiscordBotService(DiscordProperties config) {
        this.config = config;
    }

    @PostConstruct
    public void initBot() {
        if (!config.hasBotToken()) {
            log.warn("discord.bot-token is not set; bot-backed endpoints will be unavailable.");
            return;
        }
        try {
            this.jda = JDABuilder.createDefault(config.botToken())
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build()
                    .awaitReady();
            log.info("Discord bot connected as {}", jda.getSelfUser().getName());
        } catch (Exception e) {
            // Don't take the whole app down because the bot couldn't connect.
            log.error("Failed to start Discord bot; bot-backed endpoints will be unavailable.", e);
            this.jda = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            log.info("Discord bot shut down.");
        }
    }

    public boolean isAvailable() {
        return jda != null;
    }

    /** IDs of every guild the bot is currently a member of (empty if the bot is offline). */
    public Set<String> getBotGuildIds() {
        if (jda == null) {
            return Set.of();
        }
        return jda.getGuilds().stream()
                .map(Guild::getId)
                .collect(Collectors.toSet());
    }

    public List<MemberDto> getMembers(String guildId) {
        Guild guild = requireGuild(guildId);
        return loadMembers(guild).stream()
                .map(DiscordBotService::toMemberDto)
                .toList();
    }

    public List<RoleDto> getRoles(String guildId) {
        Guild guild = requireGuild(guildId);
        return guild.getRoles().stream()
                .map(DiscordBotService::toRoleDto)
                .toList();
    }

    public List<RoleDto> getUserRoles(String guildId, String userId) {
        Guild guild = requireGuild(guildId);
        Member member = requireMember(guild, userId);
        return member.getRoles().stream()
                .map(DiscordBotService::toRoleDto)
                .toList();
    }

    public void kickMember(String guildId, String userId) {
        Guild guild = requireGuild(guildId);
        Member member = requireMember(guild, userId);
        guild.kick(member).reason("Kicked via admin dashboard").queue();
    }

    public void addRole(String guildId, String userId, String roleId) {
        Guild guild = requireGuild(guildId);
        Member member = requireMember(guild, userId);
        Role role = requireRole(guild, roleId);
        guild.addRoleToMember(member, role).queue();
    }

    public void removeRole(String guildId, String userId, String roleId) {
        Guild guild = requireGuild(guildId);
        Member member = requireMember(guild, userId);
        Role role = requireRole(guild, roleId);
        guild.removeRoleFromMember(member, role).queue();
    }

    public RoleDto createRole(String guildId, String roleName, List<String> permissionNames) {
        Guild guild = requireGuild(guildId);

        List<Permission> permissions = new ArrayList<>();
        for (String name : permissionNames) {
            try {
                permissions.add(Permission.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid permission: " + name);
            }
        }

        RoleAction action = guild.createRole().setName(roleName);
        if (!permissions.isEmpty()) {
            action.setPermissions(permissions);
        }
        return toRoleDto(action.complete());
    }

    // --- helpers -----------------------------------------------------------

    private JDA requireBot() {
        if (jda == null) {
            throw new BotUnavailableException("Discord bot is not connected. Set discord.bot-token.");
        }
        return jda;
    }

    private Guild requireGuild(String guildId) {
        Guild guild = requireBot().getGuildById(guildId);
        if (guild == null) {
            throw new ResourceNotFoundException("Guild not found: " + guildId);
        }
        return guild;
    }

    private Member requireMember(Guild guild, String userId) {
        Member member = guild.getMemberById(userId);
        if (member == null) {
            throw new ResourceNotFoundException("Member not found: " + userId);
        }
        return member;
    }

    private Role requireRole(Guild guild, String roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role not found: " + roleId);
        }
        return role;
    }

    private List<Member> loadMembers(Guild guild) {
        // loadMembers() is asynchronous; bridge it to a blocking call with a timeout
        // so the controller can return a complete list.
        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        guild.loadMembers()
                .onSuccess(future::complete)
                .onError(future::completeExceptionally);
        try {
            return future.get(MEMBER_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading members", e);
        } catch (Exception e) {
            log.warn("Falling back to cached members for guild {}: {}", guild.getId(), e.getMessage());
            return guild.getMembers();
        }
    }

    private static MemberDto toMemberDto(Member member) {
        User user = member.getUser();
        return new MemberDto(
                user.getId(),
                user.getName(),
                user.getGlobalName(),
                user.getDiscriminator(),
                user.getAvatarId()
        );
    }

    private static RoleDto toRoleDto(Role role) {
        return new RoleDto(role.getId(), role.getName(), role.getColorRaw(), role.getPosition());
    }
}
