package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
import io.github.scuba10steve.s3.advanced.gui.server.MachineInterfaceMenu;
import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StevesAdvancedStorage.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
            GhostSlotFillPacket.TYPE,
            GhostSlotFillPacket.STREAM_CODEC,
            ModNetwork::handleGhostSlotFill);

        registrar.playToServer(
            AssignPatternPacket.TYPE,
            AssignPatternPacket.STREAM_CODEC,
            ModNetwork::handleAssignPattern);

        registrar.playToServer(
            UnassignPatternPacket.TYPE,
            UnassignPatternPacket.STREAM_CODEC,
            ModNetwork::handleUnassignPattern);

        registrar.playToServer(
            UpdatePatternConfigPacket.TYPE,
            UpdatePatternConfigPacket.STREAM_CODEC,
            ModNetwork::handleUpdatePatternConfig);

        registrar.playToServer(
            UpdateMachineInterfaceTickPacket.TYPE,
            UpdateMachineInterfaceTickPacket.STREAM_CODEC,
            ModNetwork::handleUpdateMachineTick);
    }

    private static void handleGhostSlotFill(GhostSlotFillPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof RecipePatternMenu menu) {
                menu.setIngredients(packet.items());
            }
        });
    }

    private static void handleAssignPattern(AssignPatternPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof RecipePatternMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            BlockEntity be = level.getBlockEntity(packet.crafterPos());
            if (be instanceof AutoCrafterBlockEntity ac) {
                ac.assign(packet.patternKey());
            } else if (be instanceof MachineInterfaceBlockEntity mi) {
                mi.setPattern(packet.patternKey());
            }
        });
    }

    private static void handleUnassignPattern(UnassignPatternPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof AutoCrafterMenu menu
                    && menu.stillValid(player)
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(packet.crafterPos()) instanceof AutoCrafterBlockEntity be) {
                be.unassign(packet.patternKey());
            }
        });
    }

    private static void handleUpdatePatternConfig(UpdatePatternConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof AutoCrafterMenu menu
                    && menu.stillValid(player)
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(packet.crafterPos()) instanceof AutoCrafterBlockEntity be) {
                int minimumBuffer = Math.max(0, Math.min(packet.minimumBuffer(), 9999));
                be.updateConfig(packet.patternKey(), packet.autoEnabled(), minimumBuffer);
            }
        });
    }

    private static void handleUpdateMachineTick(UpdateMachineInterfaceTickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof MachineInterfaceMenu menu
                    && menu.stillValid(player)
                    && player.level() instanceof ServerLevel level
                    && level.getBlockEntity(packet.pos()) instanceof MachineInterfaceBlockEntity be) {
                be.setTickInterval(Math.max(1, Math.min(packet.tickInterval(), 1200)));
            }
        });
    }
}
