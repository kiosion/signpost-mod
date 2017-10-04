package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;

public class SendAllBigPostBasesHandler implements IMessageHandler<SendAllBigPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllBigPostBasesMessage message, MessageContext ctx) {
		PostHandler.setBigPosts(message.toPostMap());
		return null;
	}
	
}