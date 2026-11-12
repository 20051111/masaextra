package com.example.masaextra.gui;

import com.example.masaextra.ExactPlaceConfig;
import com.example.masaextra.network.ServerSupport;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class GuiConfigs extends GuiConfigsBase {
    private static ConfigGuiTab tab = ConfigGuiTab.ALL;

    public GuiConfigs() {
        this(null);
    }

    public GuiConfigs(Screen parent) {
        super(10, 50, "masaextra", parent, "masaextra.gui.title.configs");
    }

    @Override
    public void initGui() {
        super.initGui();
        clearOptions();

        int x = 10;
        int y = 26;
        for (ConfigGuiTab value : ConfigGuiTab.values()) {
            ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, StringUtils.translate(value.translationKey));
            button.setEnabled(value != tab);
            addButton(button, (b, mouseButton) -> {
                tab = value;
                initGui();
            });
            x += button.getWidth() + 2;
        }
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<ConfigOptionWrapper> list = new ArrayList<>();
        switch (tab) {
            case ALL -> {
                // Every option this mod has, so this tab is a complete overview.
                // Options that integrate with another mod also appear on that mod's
                // own tab, below.
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.ENABLED));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.USE_SERVER_EXACT));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.EASY_PLACE_COOLDOWN_MS));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SUPPRESS_MISSING_SUPPORT));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE_RANGE));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_ENABLED));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_TRIGGER));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_HOTKEY));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.REFRESH_GAMMA_ON_JOIN));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.DEDUPE_TILL_SOUND));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.OMMC_HIGHLIGHT_STRICT_MATCH));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.DEBUG_MODE));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.CHAT_DEBUG));
            }
            case LITEMATICA -> {
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.USE_SERVER_EXACT));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.EASY_PLACE_COOLDOWN_MS));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SUPPRESS_MISSING_SUPPORT));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.EASY_PLACE_V2_PROTOCOL_EXTRA));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.AUTO_TOGGLE_EASY_PLACE_RANGE));
            }
            case TWEAKEROO -> {
                // Driven by Tweakeroo's snap aim switch and its gamma override.
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_ENABLED));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_TRIGGER));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SNAP_TO_BLOCK_CENTER_HOTKEY));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.DEDUPE_TILL_SOUND));
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.REFRESH_GAMMA_ON_JOIN));
            }
            case TWEAKERMORE ->
                // Driven by Tweakermore's auto-collect-material-list switch.
                list.add(new ConfigOptionWrapper(ExactPlaceConfig.SUPPRESS_EASY_PLACE_ON_AUTO_COLLECT));
            case OMMC -> list.add(new ConfigOptionWrapper(ExactPlaceConfig.OMMC_HIGHLIGHT_STRICT_MATCH));
        }
        return list;
    }

    /**
     * Status line pinned to the bottom of the screen.
     *
     * <p>Drawn rather than added as a config entry: a config entry renders as an
     * editable widget, and this value must be read-only. Drawing also keeps it out
     * of the option list and out of the saved config entirely.
     */
    @Override
    public void drawContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawContents(guiGraphics, mouseX, mouseY, partialTicks);
        this.drawString(guiGraphics, ServerSupport.getServerInfoText(), 12, this.height - 14, 0xFF9E9E9E);
    }

    @Override
    protected void onSettingsChanged() {
        super.onSettingsChanged();
        ExactPlaceConfig.INSTANCE.save();
    }

    /**
     * One tab per integrated mod, named after that mod, plus an overview tab.
     * Any new integration gets its own entry here rather than being folded into
     * an unrelated tab.
     */
    private enum ConfigGuiTab {
        ALL("masaextra.gui.tab.all"),
        LITEMATICA("masaextra.gui.tab.litematica"),
        TWEAKEROO("masaextra.gui.tab.tweakeroo"),
        TWEAKERMORE("masaextra.gui.tab.tweakermore"),
        OMMC("masaextra.gui.tab.ommc");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
