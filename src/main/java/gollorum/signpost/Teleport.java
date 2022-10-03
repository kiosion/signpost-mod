package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.ConfirmTeleportGui;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.Inventory;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.compat.ExternalWaystone;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WaystoneHandleUtils;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.ComponentSerializer;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Teleport {

    public static void toWaystone(WaystoneHandle waystone, Player player) {
        assert Signpost.getServerType().isServer;
        if(waystone instanceof WaystoneHandle.Vanilla) {
            WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData((WaystoneHandle.Vanilla) waystone);
            toWaystone(waystoneData, player);
        } else Signpost.LOGGER.error("Tried to teleport to non-vanilla waystone " + ((ExternalWaystone.Handle)waystone).modMark());
    }

    public static void toWaystone(WaystoneLocationData waystoneData, Player player){
        assert Signpost.getServerType().isServer;
        waystoneData.block.world.mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(unspecificWorld -> {
            if(!(unspecificWorld instanceof ServerLevel)) return;
            ServerLevel world = (ServerLevel) unspecificWorld;
            Vector3 location = waystoneData.spawn;
            Vector3 diff = Vector3.fromBlockPos(waystoneData.block.blockPos).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x, diff.z
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x * diff.x + diff.z * diff.z) / diff.y)));
            Level oldWorld = player.level;
            BlockPos oldPos = player.blockPosition();
            if(!player.level.dimensionType().equals(world.dimensionType()))
                player.changeDimension(world, new ITeleporter() {});
            player.setYRot(yaw.degrees());
            player.setXRot(pitch.degrees());
            player.teleportTo(location.x, location.y, location.z);
            final int steps = 6;
            TriConsumer<Level, BlockPos, Float> playStepSound = (soundWorld, pos, volume) -> {
                SoundType soundType = Blocks.STONE.defaultBlockState().getSoundType();
                soundWorld.playSound(null, pos, soundType.getStepSound(), player.getSoundSource(), soundType.getVolume() * volume, soundType.getPitch());
            };
            AtomicReference<Consumer<Integer>> playStepSounds = new AtomicReference<>();
            playStepSounds.set(countdown -> {
                float volume = countdown / (float) steps;
                playStepSound.accept(oldWorld, oldPos, volume);
                if(countdown > 1) Delay.onServerForFrames(15, () -> playStepSounds.get().accept(countdown - 1));
            });
            playStepSounds.get().accept(steps);
        });
    }

    public static void requestOnClient(
        Either<String, RequestGui.Package.Info> data,
        Optional<ConfirmTeleportGui.SignInfo> signInfo
    ) {
        ConfirmTeleportGui.display(data, signInfo);
    }

    public static ItemStack getCost(Player player, Vector3 from, Vector3 to) {
        Item item = Registry.ITEM.get(new ResourceLocation(Config.Server.teleport.costItem.get()));
        if(item.equals(Items.AIR) || player.isCreative() || player.isSpectator()) return ItemStack.EMPTY;
        int distancePerPayment = Config.Server.teleport.distancePerPayment.get();
        int distanceDependentCost = distancePerPayment < 0
            ? 0
            : (int)(from.distanceTo(to) / distancePerPayment);
        return new ItemStack(item, Config.Server.teleport.constantPayment.get() + distanceDependentCost);
    }

    public static final class Request implements PacketHandler.Event<Request.Package> {

        public static final class Package {
            public final String waystoneName;
            public final Optional<WaystoneHandle> handle;
            public Package(
                String waystoneName, Optional<WaystoneHandle> handle
            ) {
                this.waystoneName = waystoneName;
                this.handle = handle;
            }
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, FriendlyByteBuf buffer) {
            StringSerializer.instance.write(message.waystoneName, buffer);
            buffer.writeBoolean(message.handle.isPresent());
            message.handle.ifPresent(h -> h.write(buffer));
        }

        @Override
        public Package decode(FriendlyByteBuf buffer) {
            return new Package(StringSerializer.instance.read(buffer), buffer.readBoolean() ? WaystoneHandle.read(buffer) : Optional.empty());
        }

        @Override
        public void handle(
            Package message, NetworkEvent.Context context
        ) {
            ServerPlayer player = context.getSender();
            Optional<WaystoneHandle> waystone = message.handle.or(() -> WaystoneLibrary.getInstance().getHandleByName(message.waystoneName));
            Optional<WaystoneDataBase> data = waystone.flatMap(WaystoneLibrary.getInstance()::getData);
            if(data.isPresent()) {
                WaystoneHandle handle = waystone.get();
                WaystoneLocationData waystoneData = data.get().loc();

                Optional<Component> cannotTeleportBecause = WaystoneHandleUtils.cannotTeleportToBecause(player, handle, message.waystoneName);
                int distance = (int) waystoneData.spawn.distanceTo(Vector3.fromVec3d(player.position()));
                int maxDistance = Config.Server.teleport.maximumDistance.get();
                boolean isTooFarAway = maxDistance > 0 && distance > maxDistance;
                cannotTeleportBecause.ifPresent(reason -> player.sendMessage(reason, Util.NIL_UUID));
                if(isTooFarAway) player.sendMessage(new TranslatableComponent(LangKeys.tooFarAway, Integer.toString(distance), Integer.toString(maxDistance)), Util.NIL_UUID);
                if(cannotTeleportBecause.isPresent() || isTooFarAway) return;

                Inventory.tryPay(
                    player,
                    Teleport.getCost(player, Vector3.fromVec3d(player.position()), waystoneData.spawn),
                    p -> Teleport.toWaystone(waystoneData, p)
                );
            } else player.sendMessage(
                new TranslatableComponent(LangKeys.waystoneNotFound, Colors.wrap(message.waystoneName, Colors.highlight)),
                Util.NIL_UUID
            );
        }

    }

    public static final class RequestGui implements PacketHandler.Event<RequestGui.Package> {

        public static final class Package {

            public final Either<String, Info> data;
            public final Optional<PostTile.TilePartInfo> tilePartInfo;

            public Package(Either<String, Info> data, Optional<PostTile.TilePartInfo> tilePartInfo) {
                this.data = data;
                this.tilePartInfo = tilePartInfo;
            }

            public static final class Info {

                public final int maxDistance;
                public final int distance;
                public final Optional<Component> cannotTeleportBecause;
                public final String waystoneName;
                public final ItemStack cost;
                public final Optional<WaystoneHandle> handle;

                public Info(int maxDistance, int distance, Optional<Component> cannotTeleportBecause, String waystoneName, ItemStack cost, Optional<WaystoneHandle> handle) {
                    this.maxDistance = maxDistance;
                    this.distance = distance;
                    this.cannotTeleportBecause = cannotTeleportBecause;
                    this.waystoneName = waystoneName;
                    this.cost = cost;
                    this.handle = handle;
                }

                public static final Serializer serializer = new Serializer();
                public static final class Serializer implements BufferSerializable<Info> {

                    @Override
                    public Class<Info> getTargetClass() { return Info.class; }

                    @Override
                    public void write(Info info, FriendlyByteBuf buffer) {
                        buffer.writeInt(info.maxDistance);
                        buffer.writeInt(info.distance);
                        ComponentSerializer.instance.optional().write(info.cannotTeleportBecause, buffer);
                        StringSerializer.instance.write(info.waystoneName, buffer);
                        buffer.writeItem(info.cost);
                        buffer.writeOptional(info.handle, (b, h) -> h.write(b));
                    }

                    @Override
                    public Info read(FriendlyByteBuf buffer) {
                        return new Info(
                            buffer.readInt(),
                            buffer.readInt(),
                            ComponentSerializer.instance.optional().read(buffer),
                            StringSerializer.instance.read(buffer),
                            buffer.readItem(),
                            buffer.readOptional(WaystoneHandle::read).flatMap(o -> o)
                        );
                    }
                }
            }
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, FriendlyByteBuf buffer) {
            Either.BufferSerializer.of(StringSerializer.instance, Package.Info.serializer)
                .write(message.data, buffer);
            PostTile.TilePartInfo.Serializer.optional().write(message.tilePartInfo, buffer);
        }

        @Override
        public Package decode(FriendlyByteBuf buffer) {
            return new Package(
                Either.BufferSerializer.of(StringSerializer.instance, Package.Info.serializer)
                    .read(buffer),
                PostTile.TilePartInfo.Serializer.optional().read(buffer)
            );
        }

        @Override
        public void handle(Package message, NetworkEvent.Context context) {
            if(Config.Client.enableConfirmationScreen.get()) requestOnClient(
                message.data,
                message.tilePartInfo.flatMap(info -> TileEntityUtils.findTileEntity(
                    info.dimensionKey,
                    true,
                    info.pos,
                    PostTile.getBlockEntityType()
                ).flatMap(tile -> tile.getPart(info.identifier)
                    .flatMap(part -> part.blockPart instanceof SignBlockPart
                        ? Optional.of(new ConfirmTeleportGui.SignInfo(tile, (SignBlockPart) part.blockPart, info, part.offset)) : Optional.empty()
                    ))));
            else message.data.consume(
                l -> Minecraft.getInstance().player.displayClientMessage(new TranslatableComponent(l), true),
                r -> PacketHandler.sendToServer(new Request.Package(r.waystoneName, r.handle))
            );
        }
    }

}
