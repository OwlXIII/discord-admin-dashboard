package com.discordadmindashboard.controller;

import com.discordadmindashboard.dto.PermissionOption;
import net.dv8tion.jda.api.Permission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Static reference data for the UI. Currently exposes the set of assignable
 * Discord permissions, sourced directly from JDA so it stays in sync with what
 * {@code createRole} accepts.
 */
@RestController
@RequestMapping("/api")
public class ReferenceController {

    @GetMapping("/permissions")
    public List<PermissionOption> permissions() {
        return java.util.Arrays.stream(Permission.values())
                .filter(p -> p != Permission.UNKNOWN)
                .map(p -> new PermissionOption(p.name(), p.getName()))
                .sorted(Comparator.comparing(PermissionOption::label))
                .toList();
    }
}
