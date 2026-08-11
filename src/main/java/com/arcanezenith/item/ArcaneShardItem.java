package com.arcanezenith.item;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 * Arcane Points in physical item form. Right-click to consume and gain points toward
 * unlocking spells. Dropped by hostile mobs and found in chests inside Arcane structures
 * (see the loot modifier + LivingDeathEvent handler for the actual sources).
 */
public class ArcaneShardItem extends Item {

    public static final int POINTS_GRANTED = 10;

    public ArcaneShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            var progress = serverPlayer.getData(ModAttachments.SPELL_PROGRESS);
            progress.addPoints(POINTS_GRANTED);
            stack.shrink(1);

            ((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);

            ModNetworking.syncProgression(serverPlayer);
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§d+" + POINTS_GRANTED + " Arcane Points"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
