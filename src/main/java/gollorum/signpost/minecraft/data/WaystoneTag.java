package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

public class WaystoneTag extends ItemTagsProvider {

    public static final String Id = "waystone";

    public static final Tag.Named<Item> Tag = ItemTags.bind(Id);

    public WaystoneTag(DataGenerator dataGenerator, WaystoneTag.Blocks blockTagProvider, ExistingFileHelper fileHelper) {
        super(dataGenerator, blockTagProvider, Signpost.MOD_ID, fileHelper);
    }

    @Override
    protected void addTags() {
        this.tag(Tag)
            .add(ModelWaystone.variants.stream().map(i -> i.block.asItem()).toArray(Item[]::new))
            .add(WaystoneBlock.INSTANCE.asItem());
    }

    public static class Blocks extends BlockTagsProvider {

        public static final Tag.Named<Block> Tag = BlockTags.bind(Id);

        public Blocks(DataGenerator generatorIn, ExistingFileHelper fileHelper) {
            super(generatorIn, Signpost.MOD_ID, fileHelper);
        }

        @Override
        protected void addTags() {
            this.tag(Tag)
                .add(ModelWaystone.variants.stream().map(i -> i.block).toArray(Block[]::new))
                .add(WaystoneBlock.INSTANCE);
        }

    }

}