package org.semakol.hudglassescc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.block.entity.HudModemBlockEntity;
import org.semakol.hudglassescc.item.BoundModemData;

import java.util.List;

public class HudModemBlock extends BaseEntityBlock {
    public static final MapCodec<HudModemBlock> CODEC = simpleCodec(HudModemBlock::new);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 6, 14);

    public HudModemBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HudModemBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Hudglassescc.HUD_GLASSES.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof HudModemBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Binding lives entirely on the glasses stack: toggle this modem's id on it.
        int beId = be.getModemId();
        BoundModemData existing = stack.get(Hudglassescc.BOUND_MODEM.get());
        boolean stackBoundHere = existing != null && existing.modemId() == beId;

        if (stackBoundHere) {
            stack.remove(Hudglassescc.BOUND_MODEM.get());
            player.displayClientMessage(Component.translatable("hudglassescc.modem.unbound"), true);
        } else {
            stack.set(Hudglassescc.BOUND_MODEM.get(), new BoundModemData(beId));
            player.displayClientMessage(
                    Component.translatable("hudglassescc.modem.bound", beId), true);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof HudModemBlockEntity be)) return InteractionResult.PASS;

        List<String> viewers = be.getCurrentViewers();
        if (viewers.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("hudglassescc.modem.status_idle", be.getModemId()), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("hudglassescc.modem.status_viewers",
                            be.getModemId(), String.join(", ", viewers)), true);
        }
        return InteractionResult.CONSUME;
    }
}
