package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.Sign.OverlayType;
import net.minecraft.tileentity.TileEntity;

public class SendPostBasesHandler implements IMessageHandler<SendPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendPostBasesMessage message, MessageContext ctx) {
		TileEntity tile = message.pos.getTile();
		DoubleBaseInfo bases;
		if(tile instanceof PostPostTile){
			((PostPostTile)tile).isWaystone();
			bases = ((PostPostTile)tile).getBases();
		}else{
			bases = PostHandler.getPosts().get(message.pos);
			if(bases==null){
				bases = new DoubleBaseInfo(null, null);
				PostHandler.getPosts().put(message.pos, bases);
			}
		}
		bases.sign1.rotation = message.base1rot;
		bases.sign2.rotation = message.base2rot;
		bases.sign1.flip = message.flip1;
		bases.sign2.flip = message.flip2;
		bases.sign1.base = PostHandler.getForceWSbyName(message.base1);
		bases.sign2.base = PostHandler.getForceWSbyName(message.base2);
		
		bases.sign1.overlay = OverlayType.get(message.overlay1);
		bases.sign2.overlay = OverlayType.get(message.overlay2);
		
		bases.sign1.point = message.point1;
		bases.sign2.point = message.point2;
		bases.sign1.paint = SuperPostPostTile.stringToLoc(message.paint1);
		bases.sign2.paint = SuperPostPostTile.stringToLoc(message.paint2);
		
		bases.postPaint = SuperPostPostTile.stringToLoc(message.postPaint);
		if(ctx.side.equals(Side.SERVER)){
			ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.pos.x, message.pos.y, message.pos.z).markDirty();
			NetworkHandler.netWrap.sendToAll(message);
		}
		return null;
	}

}
