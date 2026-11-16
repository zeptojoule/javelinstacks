package com.jaca.javelinstacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regra: uma pilha com mais de 1 javelin só existe enquanto eles estão intactos.
 * Quando um javelin da pilha toma dano, ele sai da pilha como item separado
 * (guardando a própria durabilidade) e o resto continua intacto.
 *
 * Como o dano é aplicado pelo mod do javelin na pilha inteira, comparamos com o
 * estado do tick anterior para saber que foi UMA unidade que tomou o dano.
 */
public final class JavelinSplitter {
    private record Snap(Item item, int count, int damage) {}

    private static final Map<UUID, Snap[]> SNAPS = new HashMap<>();

    private JavelinSplitter() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Inventory inv = player.getInventory();
        int size = inv.getContainerSize();

        Snap[] prev = SNAPS.get(player.getUUID());
        if (prev == null || prev.length != size) {
            prev = new Snap[size];
            SNAPS.put(player.getUUID(), prev);
        }

        List<ItemStack> extras = null;

        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !JavelinStacks.isJavelin(stack.getItem())) continue;
            if (stack.getCount() <= 1 || !stack.isDamaged()) continue;

            if (extras == null) extras = new ArrayList<>();

            Snap p = prev[i];
            int count = stack.getCount();

            if (p != null && p.item() == stack.getItem() && p.count() == count && p.damage() == 0) {
                // Uma unidade da pilha intacta tomou dano: ela sai da pilha.
                ItemStack used = stack.copyWithCount(1);
                stack.setDamageValue(0);
                stack.shrink(1);
                extras.add(used);
            } else {
                // Pilha danificada de origem desconhecida (ex.: itens iguais que se juntaram no chão):
                // todas as unidades têm o mesmo dano, então viram itens separados, sem perder nada.
                for (int n = 1; n < count; n++) {
                    extras.add(stack.copyWithCount(1));
                }
                stack.setCount(1);
            }
        }

        if (extras != null) {
            for (ItemStack extra : extras) {
                if (!inv.add(extra)) {
                    player.drop(extra, false);
                }
            }
        }

        // Guarda o estado final deste tick para comparar no próximo.
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && JavelinStacks.isJavelin(stack.getItem())) {
                prev[i] = new Snap(stack.getItem(), stack.getCount(), stack.getDamageValue());
            } else {
                prev[i] = null;
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNAPS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SNAPS.clear();
    }
}
