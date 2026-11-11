package com.example.masaextra;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Prints Exact Place trigger details to the chat when the chat-debug option is
 * enabled. Kept separate from the log output so it can be toggled on its own.
 */
public final class ChatDebug {
    private ChatDebug() {
    }

    public static void state(String prefix, BlockState state, BlockPos pos) {
        if (!ExactPlaceConfig.CHAT_DEBUG.getBooleanValue()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage(
                Component.literal("[" + prefix + "] ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(describe(state, pos))
                                .withStyle(ChatFormatting.WHITE)),
                false);
    }

    public static void info(String prefix, String message) {
        if (!ExactPlaceConfig.CHAT_DEBUG.getBooleanValue()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage(
                Component.literal("[" + prefix + "] " + message)
                        .withStyle(ChatFormatting.AQUA),
                false);
    }

    /** "minecraft:redstone_wall_torch[facing=north,lit=true] @ 100 26 -514 (block=..., id=...)" */
    public static String describe(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(properties(state));
        if (pos != null) {
            sb.append(" @ ").append(pos.getX()).append(' ')
                    .append(pos.getY()).append(' ').append(pos.getZ());
        }
        sb.append(" (class=").append(block.getClass().getSimpleName()).append(')');
        return sb.toString();
    }

    private static String properties(BlockState state) {
        if (state.getValues().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Property<?> property : state.getProperties()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(property.getName()).append('=').append(valueOf(state, property));
        }
        return sb.append(']').toString();
    }

    private static <T extends Comparable<T>> String valueOf(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
