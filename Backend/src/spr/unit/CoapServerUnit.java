package spr.unit;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapServer;

import dave.json.Container;
import dave.json.JsonArray;
import dave.json.JsonCollectors;
import dave.json.JsonNumber;
import dave.json.JsonObject;
import dave.json.JsonValue;
import dave.json.Loader;
import dave.json.Saveable;
import dave.json.Saver;
import spr.net.common.Message;
import spr.net.common.Node;
import spr.task.Task;
import spr.task.Tasks;

public class CoapServerUnit extends BaseUnit
{
	private final CoapServer mServer;

	public CoapServerUnit(CoapServer s, Node<Task> g)
	{
		super(g);
		
		mServer = s;
		
		registerMessageHandler(Tasks.Coap.SEND, this::handleSend);
	}
	
	@Override
	protected void onStart( )
	{
		mServer.start();
	}
	
	@Override
	protected void onStop( )
	{
		mServer.stop();
	}
	
	private void handleSend(Message<Task> p)
	{
		Packet msg = p.getContent().getPayload();
		
		CoapClient c = new CoapClient(msg.getURI());
		
		switch(msg.action)
		{
			case GET:
				c.get();
				break;

			case POST:
				c.post(msg.payload, 0);
				break;

			case PUT:
				c.put(msg.payload, 0);
				break;

			case DELETE:
				c.delete();
				break;
				
		}
	}
	
	@Container
	public static final class Packet implements Saveable
	{
		public final String host;
		public final int port;
		public final String path;
		public final byte[] payload;
		public final Directive action;
		
		public Packet(String host, int port, String path, byte[] payload, Directive action)
		{
			this.host = host;
			this.port = port;
			this.path = path;
			this.payload = payload;
			this.action = action;
		}
		
		public String getURI( ) { return "coap://" + host + ":" + port + "/" + path; }
		
		@Override
		@Saver
		public JsonValue save( )
		{
			JsonObject json = new JsonObject();
			
			json.putString("host", host);
			json.putInt("port", port);
			json.putString("path", path);
			json.put("payload", IntStream.range(0, payload.length).mapToObj(i -> new JsonNumber(payload[i])).collect(JsonCollectors.ofArray()));
			json.putString("action", action.toString());
			
			return json;
		}
		
		@Loader
		public static Packet load(JsonValue json)
		{
			JsonObject o = (JsonObject) json;
			
			String host = o.getString("host");
			int port = o.getInt("port");
			String path = o.getString("path");
			Directive action = Directive.valueOf(o.getString("action"));
			
			JsonArray p = o.getArray("payload");
			ByteBuffer bb = ByteBuffer.allocate(p.size());
			
			p.stream().forEach(v -> bb.put((byte) ((JsonNumber) v).getInt()));
			
			byte[] payload = bb.array();
			
			return new Packet(host, port, path, payload, action);
		}
		
		@Override
		public String toString( )
		{
			return getURI() + "[" + action + " " + payload.length + "B]";
		}
	}
	
	public static enum Directive
	{
		GET,
		POST,
		PUT,
		DELETE
	}
}
