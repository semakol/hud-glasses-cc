package org.semakol.hudglassescc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.block.entity.HudModemBlockEntity;
import org.semakol.hudglassescc.compat.CuriosCompat;
import org.semakol.hudglassescc.item.BoundModemData;

import java.util.List;

public class HudModemBlock extends BaseEntityBlock {
    public static final MapCodec<HudModemBlock> CODEC = simpleCodec(HudModemBlock::new);

    /** Direction the modem's screen faces = the block face it is stuck to. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // A 3-thick slab flush against the attachment surface (opposite the screen).
    // FACING = screen direction, so the slab sits on the side toward -FACING.
    private static final VoxelShape SHAPE_UP    = Block.box(1, 0, 1, 15, 3, 15);    // attach below
    private static final VoxelShape SHAPE_DOWN  = Block.box(1, 13, 1, 15, 16, 15);  // attach above
    private static final VoxelShape SHAPE_NORTH = Block.box(1, 1, 13, 15, 15, 16);  // attach south
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 1, 0, 15, 15, 3);    // attach north
    private static final VoxelShape SHAPE_WEST  = Block.box(13, 1, 1, 16, 15, 15);  // attach east
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 1, 1, 3, 15, 15);    // attach west

    public HudModemBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Stick to whichever face was clicked, screen pointing outward.
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HudModemBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(FACING)) {
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_UP;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
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
        toggleBinding(player, stack, be.getModemId());
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof HudModemBlockEntity be)) return InteractionResult.PASS;

        // Empty hand but glasses are already worn (helmet or a Curios slot) →
        // bind/unbind the worn pair without taking them off.
        ItemStack worn = CuriosCompat.getWornHudStack(player);
        if (worn.is(Hudglassescc.HUD_GLASSES.get())) {
            toggleBinding(player, worn, be.getModemId());
            return InteractionResult.CONSUME;
        }

        // Otherwise just report status.
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

    /** Toggles this modem's id on the given glasses stack (binding lives on the item). */
    private static void toggleBinding(Player player, ItemStack glasses, int modemId) {
        BoundModemData existing = glasses.get(Hudglassescc.BOUND_MODEM.get());
        if (existing != null && existing.modemId() == modemId) {
            glasses.remove(Hudglassescc.BOUND_MODEM.get());
            player.displayClientMessage(Component.translatable("hudglassescc.modem.unbound"), true);
        } else {
            glasses.set(Hudglassescc.BOUND_MODEM.get(), new BoundModemData(modemId));
            player.displayClientMessage(Component.translatable("hudglassescc.modem.bound", modemId), true);
        }
    }
}
