package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;

import java.util.Collection;

public class GuiBlockPartRenderer extends AbstractWidget {

    private final Collection<BlockPartInstance> partsToRender;
    private final Point center;
    private Angle yaw;
    private Angle pitch;
    private float scale;

    public GuiBlockPartRenderer(Collection<BlockPartInstance> partsToRender, Point center, Angle yaw, Angle pitch, float scale) {
        super(center.x - widthFor(scale) / 2, center.y - heightFor(scale) / 2, widthFor(scale), heightFor(scale), new TextComponent(""));
        this.partsToRender = partsToRender;
        this.center = center;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
    }

    private static int widthFor(float scale) { return (int)(scale * 1.5f); }
    private static int heightFor(float scale) { return (int)(scale * 1.5f); }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if(isHovered())
            GuiComponent.fill(matrixStack, x, y, x + width, y + height, 0x20ffffff);
        for(BlockPartInstance bpi : partsToRender) {
            BlockPartRenderer.renderGuiDynamic(
                bpi.blockPart,
                center, yaw, pitch, scale, bpi.offset.add(0, -0.5f, 0.5f)
            );
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        yaw = yaw.add(Angle.fromDegrees((float) (dragX * 3)));
        pitch = pitch.add(Angle.fromDegrees((float) (dragY * 3)));
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
