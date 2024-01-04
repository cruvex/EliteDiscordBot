package com.cruvex.commands;

/**
 * An enum containing categories for commands.
 */
public enum CommandGroup {
    GENERAL    ("General"),
    HELP       ("Help"),
    MODERATION ("Moderation");

    private final String commandGroup;

    CommandGroup(String commandGroup) {
        this.commandGroup = commandGroup;
    }

    @Override
    public String toString() {
        return commandGroup;
    }
}