package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RenderingUtil {
    
    private static ModelBakery modelBakery = Minecraft.getInstance().getModelManager().getModelBakery();

    public static BakedModel loadModel(ResourceLocation location) {
        return modelBakery.bake(
            location,
            new SimpleModelState(Transformation.identity()),
            Material::sprite
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return modelBakery.getModel(modelLocation).bake(
            modelBakery,
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(textLoc),
            new SimpleModelState(Transformation.identity()),
            modelLocation
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return modelBakery.getModel(modelLocation).bake(
            modelBakery,
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(
                m.texture().equals(PostModel.mainTextureMarker)
                    ? textLoc1 : textLoc2
            ),
            new SimpleModelState(Transformation.identity()),
            modelLocation
        );
    }

    private static final Lazy<ModelBlockRenderer> Renderer = Lazy.of(() -> Minecraft.getInstance().getBlockRenderer().getModelRenderer());

    public static interface RenderModel {
        void render(
            BakedModel model,
            Level world,
            BlockState state,
            BlockPos pos,
            VertexConsumer buffer,
            boolean checkSides,
            RandomSource random,
            long rand,
            int combinedOverlay,
            Matrix4f rotationMatrix
        );
    }

    public static ResourceLocation trim(ResourceLocation textureLocation){
        if(textureLocation.getPath().startsWith("textures/"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring("textures/".length()));
        if(textureLocation.getPath().endsWith(".png"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring(0, textureLocation.getPath().length() - ".png".length()));
        return textureLocation;
    }

    public static void render(PoseStack matrix, Consumer<RenderModel> inner){
        matrix.pushPose();
        inner.accept((model, world, state, pos, buffer, checkSides, random, rand, combinedOverlay, rotationMatrix) -> {
            Renderer.get().tesselateBlock(
                world,
                model,
                state,
                pos,
                matrix,
                buffer,
                checkSides,
                random,
                rand,
                combinedOverlay
            );
        });
        matrix.popPose();
    }

    public static int drawString(Font fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        int textWidth = fontRenderer.width(text);
        float scale = Math.min(1f, maxWidth / (float) textWidth);
        Matrix4f matrix = Matrix4f.createTranslateMatrix(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f,
            100
        );
        if(scale < 1) matrix.multiply(Matrix4f.createScaleMatrix(scale, scale, scale));
        int i = fontRenderer.drawInBatch(
            text,
            (maxWidth - Math.min(maxWidth, textWidth)) * 0.5f,
            -fontRenderer.lineHeight * 0.5f,
            color,
            dropShadow,
            matrix,
            buffer,
            false,
            0,
            0xf000f0
        );
        buffer.endBatch();
        return i;
    }

    public static void renderGui(BakedModel model, PoseStack matrixStack, int color, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, RenderType renderType, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(center.x, center.y, 0);
            matrixStack.scale(scale, -scale, scale);
            matrixStack.mulPose(new Quaternion(Vector3f.XP, pitch.radians(), false));
            if(isFlipped) matrixStack.mulPose(new Quaternion(Vector3f.YP, (float) Math.PI, false));
            MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            renderGui(model, matrixStack, color, offset, yaw, renderTypeBuffer.getBuffer(renderType), renderType, 0xf000f0, OverlayTexture.NO_OVERLAY, alsoDo);
            renderTypeBuffer.endBatch();
        });
    }

    public static void renderGui(BakedModel model, PoseStack matrixStack, int color, Vector3 offset, Angle yaw, VertexConsumer builder, RenderType renderType, int combinedLight, int combinedOverlay, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.mulPose(new Quaternion(Vector3f.YP, yaw.radians(), false));
            matrixStack.translate(offset.x, offset.y, offset.z);
            wrapInMatrixEntry(matrixStack, () -> {

                List<Direction> allDirections = new ArrayList<>(Arrays.asList(Direction.values()));
                allDirections.add(null);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
                float r = Colors.getRed(color) / 255f;
                float g = Colors.getGreen(color) / 255f;
                float b = Colors.getBlue(color) / 255f;
                RandomSource random = RandomSource.create();
                for(Direction dir : allDirections) {
                    random.setSeed(42L);
                    for(BakedQuad quad: model.getQuads(null, dir, random, ModelData.EMPTY, renderType)) {
                        builder.putBulkData(matrixStack.last(), quad, r, g, b, combinedLight, combinedOverlay);
                    }

                }
            });

            alsoDo.accept(matrixStack);
        });
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

    public static BakedModel withTintIndex(BakedModel original, int tintIndex) {
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
                return original.getQuads(state, side, rand)
                       .stream().map(q -> new BakedQuad(q.getVertices(), tintIndex, q.getDirection(), q.getSprite(), q.isShade()))
                       .collect(Collectors.toList());
            }

            @Override
            public boolean useAmbientOcclusion() {
                return original.useAmbientOcclusion();
            }

            @Override
            public boolean isGui3d() {
                return original.isGui3d();
            }

            @Override
            public boolean usesBlockLight() {
                return original.usesBlockLight();
            }

            @Override
            public boolean isCustomRenderer() {
                return original.isCustomRenderer();
            }

            @Override
            public TextureAtlasSprite getParticleIcon() {
                return original.getParticleIcon();
            }

            @Override
            public ItemOverrides getOverrides() {
                return original.getOverrides();
            }
        };
    }

}
