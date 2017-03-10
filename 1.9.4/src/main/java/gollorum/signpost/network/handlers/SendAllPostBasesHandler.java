package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage.DoubleStringInt;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllPostBasesHandler implements IMessageHandler<SendAllPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllPostBasesMessage message, MessageContext ctx) {
		for(Entry<MyBlockPos, DoubleStringInt> now : message.posts.entrySet()){
			boolean found = false;
			for(Entry<MyBlockPos, DoubleBaseInfo> nowPost: PostHandler.posts.entrySet()){
				if(nowPost.getKey().equals(now.getKey())){
					found = true;

					nowPost.getValue().base1 = PostHandler.getWSbyName(now.getValue().string1);
					nowPost.getValue().base2 = PostHandler.getWSbyName(now.getValue().string2);

					nowPost.getValue().rotation1 = now.getValue().int1;
					nowPost.getValue().rotation2 = now.getValue().int2;

					nowPost.getValue().flip1 = now.getValue().bool1;
					nowPost.getValue().flip2 = now.getValue().bool2;
					
					break;
				}
			}
			if(!found){
				PostHandler.posts.put(now.getKey(), new DoubleBaseInfo(PostHandler.getWSbyName(now.getValue().string1), PostHandler.getWSbyName(now.getValue().string2), now.getValue().int1, now.getValue().int2, now.getValue().bool1, now.getValue().bool2));
			}
		}
		PostHandler.posts.keepSame(message.posts);
		return null;
	}
	
}
		