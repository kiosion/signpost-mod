package gollorum.signpost.network.messages;

import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.ConfigHandler.RecipeCost;
import gollorum.signpost.management.ConfigHandler.SecurityLevel;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class InitPlayerResponseMessage implements IMessage {

	public StonedHashSet allWaystones = new StonedHashSet();

	public boolean deactivateTeleportation;
	public boolean interdimensional;
	public int maxDist;
	public String paymentItem;
	public int costMult;

	public RecipeCost signRec;
	public RecipeCost waysRec;

	public SecurityLevel securityLevelWaystone;
	public SecurityLevel securityLevelSignpost;

	public float villageWaystonePropability;
	public int villageMinSignposts;
	public int villageMaxSignposts;
	public boolean onlyVillageTargets;

	public InitPlayerResponseMessage() {
		if (!ConfigHandler.isDeactivateTeleportation()) {
			allWaystones = PostHandler.getNativeWaystones();
		} 
	    deactivateTeleportation = ConfigHandler.isDeactivateTeleportation(); 
	    interdimensional = ConfigHandler.isInterdimensional(); 
	    maxDist = ConfigHandler.getMaxDist(); 
	    paymentItem = ConfigHandler.getPaymentItem(); 
	    costMult = ConfigHandler.getCostMult(); 
	    signRec = ConfigHandler.getSignRec(); 
	    waysRec = ConfigHandler.getWaysRec(); 
	    securityLevelWaystone = ConfigHandler.getSecurityLevelWaystone(); 
	    securityLevelSignpost = ConfigHandler.getSecurityLevelSignpost(); 
	    villageWaystonePropability = ConfigHandler.getVillageWaystonePropability(); 
	    villageMinSignposts = ConfigHandler.getVillageMinSignposts(); 
	    villageMaxSignposts = ConfigHandler.getVillageMaxSignposts(); 
	    onlyVillageTargets = ConfigHandler.isOnlyVillageTargets(); 
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(deactivateTeleportation);
		if (!ConfigHandler.isDeactivateTeleportation()) {
			buf.writeInt(allWaystones.size());
			for (BaseInfo now : allWaystones) {
				now.toBytes(buf);
			}
		}
		buf.writeBoolean(interdimensional);
		buf.writeInt(maxDist);
		ByteBufUtils.writeUTF8String(buf, paymentItem);
		buf.writeInt(costMult);
		ByteBufUtils.writeUTF8String(buf, signRec.name());
		ByteBufUtils.writeUTF8String(buf, waysRec.name());
		ByteBufUtils.writeUTF8String(buf, securityLevelWaystone.name());
		ByteBufUtils.writeUTF8String(buf, securityLevelSignpost.name()); 
	    buf.writeFloat(villageWaystonePropability); 
	    buf.writeInt(villageMinSignposts); 
	    buf.writeInt(villageMaxSignposts); 
	    buf.writeBoolean(onlyVillageTargets); 
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		deactivateTeleportation = buf.readBoolean();
		if (!deactivateTeleportation) {
			allWaystones = new StonedHashSet();
			int c = buf.readInt();
			for (int i = 0; i < c; i++) {
				allWaystones.add(BaseInfo.fromBytes(buf));
			}
		}
		interdimensional = buf.readBoolean();
		maxDist = buf.readInt();
		paymentItem = ByteBufUtils.readUTF8String(buf);
		costMult = buf.readInt();
		signRec = RecipeCost.valueOf(ByteBufUtils.readUTF8String(buf));
		waysRec = RecipeCost.valueOf(ByteBufUtils.readUTF8String(buf));
		securityLevelWaystone = SecurityLevel.valueOf(ByteBufUtils.readUTF8String(buf));
		securityLevelSignpost = SecurityLevel.valueOf(ByteBufUtils.readUTF8String(buf)); 
	    villageWaystonePropability = buf.readFloat(); 
	    villageMinSignposts = buf.readInt(); 
	    villageMaxSignposts = buf.readInt(); 
	    onlyVillageTargets = buf.readBoolean(); 
	}

}
