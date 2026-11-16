package com.jaca.javelinstacks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@Mod(JavelinStacks.MOD_ID)
public class JavelinStacks {
    public static final String MOD_ID = "javelinstacks";

    /** Sufixo do ID (sem o namespace) que marca um item como javelin. */
    public static final String SUFFIX = "_javelin";

    /** Tamanho máximo da pilha. */
    public static final int MAX_STACK = 8;

    private static final Map<Item, Boolean> CACHE = new ConcurrentHashMap<>();

    public JavelinStacks(IEventBus modBus) {
        modBus.addListener(JavelinStacks::onModifyComponents);

        NeoForge.EVENT_BUS.addListener(JavelinSplitter::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(JavelinSplitter::onLogout);
        NeoForge.EVENT_BUS.addListener(JavelinSplitter::onServerStopped);
    }

    public static boolean isJavelin(Item item) {
        return CACHE.computeIfAbsent(item,
                i -> BuiltInRegistries.ITEM.getKey(i).getPath().endsWith(SUFFIX));
    }

    private static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(JavelinStacks::isJavelin,
                builder -> builder.set(DataComponents.MAX_STACK_SIZE, MAX_STACK));
    }
}
