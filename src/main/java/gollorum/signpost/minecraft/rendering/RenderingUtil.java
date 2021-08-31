package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RenderingUtil {

    public static BakedModel loadModel(ResourceLocation location) {
        return ModelLoader.instance().bake(
            location,
            new SimpleModelState(Transformation.identity()),
            Material::sprite
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return ModelLoader.instance().getModel(modelLocation).bake(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(textLoc),
            new SimpleModelState(Transformation.identity()),
            modelLocation
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return ModelLoader.instance().getModel(modelLocation).bake(
            ModelLoader.instance(),
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
            BlockEntity tileEntity,
            VertexConsumer buffer,
            boolean checkSides,
            Random random,
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
        inner.accept((model, tileEntity, buffer, checkSides, random, rand, combinedOverlay, rotationMatrix) -> {
            if(!tileEntity.hasLevel()) throw new RuntimeException("TileEntity without world cannot be rendered.");
            Renderer.get().tesselateBlock(
                tileEntity.getLevel(),
                model,
                tileEntity.getBlockState(),
                tileEntity.getBlockPos(),
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

    public static void renderGui(BakedModel model, Point center, Angle yaw, Angle pitch, float scale, Vector3 offset) {
        PoseStack matrixStack = new PoseStack();
        Minecraft.getInstance().getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, false);
        matrixStack.translate(center.x, center.y, 100);
        matrixStack.scale(scale, -scale, scale);
        matrixStack.mulPose(new Quaternion(Vector3f.XP, pitch.radians(), false));
        matrixStack.mulPose(new Quaternion(Vector3f.YP, yaw.radians(), false));
        matrixStack.translate(offset.x, offset.y, offset.z);
        matrixStack.translate(0.5f, 0.5f, 0);
        MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

        Minecraft.getInstance().getItemRenderer().render(
            new ItemStack(Blocks.OAK_LOG),
            ItemTransforms.TransformType.GUI,
            false,
            matrixStack,
            renderTypeBuffer,
            0xf000f0,
            OverlayTexture.NO_OVERLAY,
            model
        );
        renderTypeBuffer.endBatch();
    }

    public static BakedModel withTintIndex(BakedModel original, int tintIndex) {
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
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
