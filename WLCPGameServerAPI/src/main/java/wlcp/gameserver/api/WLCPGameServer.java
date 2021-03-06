package wlcp.gameserver.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import wlcp.gameserver.api.exception.CouldNotConnectToWLCPException;
import wlcp.shared.packet.IPacket;
import wlcp.shared.packet.PacketTypes;
import wlcp.shared.packets.ConnectAcceptedPacket;
import wlcp.shared.packets.ConnectPacket;
import wlcp.shared.packets.ConnectRejectedPacket;
import wlcp.shared.packets.DisplayTextPacket;
import wlcp.shared.packets.GameLobbiesPacket;
import wlcp.shared.packets.GameTeamsPacket;
import wlcp.shared.packets.HeartBeatPacket;
import wlcp.shared.packets.KeyboardInputPacket;
import wlcp.shared.packets.SequenceButtonPressPacket;
import wlcp.shared.packets.SingleButtonPressPacket;

/**
 * This class implements methods defined in the IWLCPGameServer interface.
 * The purpose of this class is to provide connectivity and to the server
 * as well as a way to interact with it.
 * @author Matthew Micciolo
 *
 */
public class WLCPGameServer extends Thread implements IWLCPGameServer  {
	
	private String ipAddress;
	private int ipPort;
	private AsynchronousSocketChannel channel;
	private WLCPGameServerListener listener;
	private ConcurrentLinkedQueue<ByteBuffer> recievedPackets = new ConcurrentLinkedQueue<ByteBuffer>();
	private final Semaphore available = new Semaphore(1, true);
	private Server server;
	
	public WLCPGameServer(String ipAddress, int ipPort) {
		this.ipAddress = ipAddress;
		this.ipPort = ipPort;
	}
	
	/**
	 * Call this method in order to attempt to make a TCP connection to the game server.
	 * If the call succeeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public <A> void connect(final CompletionHandler<Void, ? super A> completionHandler, final A attachment) {
		
		//Open up an async socket channel
		try {
			channel = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			completionHandler.failed(e, attachment);
			return;
		}
		
		//Connect
		channel.connect(new InetSocketAddress(ipAddress, ipPort), this, new CompletionHandler<Void, WLCPGameServer>() {
            
            public void completed(Void result, WLCPGameServer wlcpGameServer ) {  
            	
            	//Start the processing thread
            	wlcpGameServer.start();
            	
    			//Create a new container for server information
    			server = new Server(wlcpGameServer.channel, wlcpGameServer);
    			
    			//Create a new handler to handle reads and writes
    			ReadWriteHandler readWriteHandler = new ReadWriteHandler();
    			
    			//Go into read mode
    			wlcpGameServer.channel.read(server.buffer, server, readWriteHandler);
    			
    			//Call the users completion handler
    			completionHandler.completed(result, attachment);
            }

            
            public void failed(Throwable exc, WLCPGameServer channel) {
            	//Call the users completion handler
                completionHandler.failed(new CouldNotConnectToWLCPException("IOException. Could not connect to server. Verify ip and port are set correctly."), attachment);
            }});
		} 
	
	/**
	 * Call this method to disconnect the TCP socket connection.
	 * If the call succeeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public <A> void disconnect(CompletionHandler<Void, ? super A> completionHandler, A attachment) {
		
		//Try to close the channel
		try {
			//Success
			channel.close();
			completionHandler.completed(null, attachment);
		} catch (IOException e) {
			//Failure
			completionHandler.failed(e, attachment);
		}
	}
	
	/**
	 * Main packet processing queue. Pull packets off as they are 
	 * available and process them.
	 */
	public void run() {
		while(true) {
			long startTime = System.currentTimeMillis();
			try {
				accquire();
				for(ByteBuffer byteBuffer : recievedPackets) {
					handlePacket(byteBuffer);
					recievedPackets.remove(byteBuffer);
				}
				release();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long finalTime = System.currentTimeMillis() - startTime;
			try {
				if(16 - finalTime > 0) {
					Thread.sleep(16 - finalTime);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Call this method in order to send a packet to the game server. Any type of IPacket can be sent.
	 * These packets are defined in the WLCP Shared library.
	 * If the call succeeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public <A> void SendPacket(final IPacket packet, final CompletionHandler<Void, ? super A> completionHandler, final A attachment) {
		final ByteBuffer byteBuffer = packet.assemblePacket();
		channel.write(packet.assemblePacket(), channel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {
            
            public void completed(Integer result, AsynchronousSocketChannel channel ) {
            	if(result != packet.getPacketSize()) {
            		//Keep writing
            		channel.write(byteBuffer, channel, this);
            	}
            	completionHandler.completed(null, attachment);
            }

            
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
            	completionHandler.failed(exc, attachment);
            }
		});
	}
	
	/**
	 * This method registers a WLCPGameServerListener. This is a collection of methods which
	 * will be called when certain packets from the server are recieved. The API provides
	 * a base implementation of a listener called WLCPBaseGameServerListener that should be
	 * used in most cases.
	 */
	public void registerEventListener(WLCPGameServerListener listener) {
		this.listener = listener;
	}
	
	/**
	 * This private method is called by the recieved packets queue in run().
	 * It will distribute the packets recieved to the appropriate listener.
	 * @param byteBuffer
	 */
	private void handlePacket(ByteBuffer byteBuffer) {
		byteBuffer.flip();
		switch(PacketTypes.values()[byteBuffer.get(0)]) {
		case HEARTBEAT:
			HeartBeatPacket heartBeatPacket = new HeartBeatPacket();
			heartBeatPacket.populateData(byteBuffer);
			listener.recievedHearbeat(this, heartBeatPacket);
			break;
		case GAME_LOBBIES:
			GameLobbiesPacket gameLobbiesPacket = new GameLobbiesPacket();
			gameLobbiesPacket.populateData(byteBuffer);
			listener.gameLobbiesRecieved(this, gameLobbiesPacket);
			break;
		case GAME_TEAMS:
			GameTeamsPacket gameTeamsPacket = new GameTeamsPacket();
			gameTeamsPacket.populateData(byteBuffer);
			listener.gameTeamsRecieved(this, gameTeamsPacket);
			break;
		case CONNECT_ACCEPTED:
			ConnectAcceptedPacket connectAcceptedPacket = new ConnectAcceptedPacket();
			connectAcceptedPacket.populateData(byteBuffer);
			listener.connectToGameAccepted(this, connectAcceptedPacket);
			break;
		case CONNECT_REJECTED:
			ConnectRejectedPacket connectRejectedPacket = new ConnectRejectedPacket();
			connectRejectedPacket.populateData(byteBuffer);
			listener.connectToGameRejected(this, connectRejectedPacket);
			break;
		case DISPLAY_TEXT:
			DisplayTextPacket displayTextPacket = new DisplayTextPacket();
			displayTextPacket.populateData(byteBuffer);
			listener.recievedDisplayText(this, displayTextPacket);
			break;
		case SINGLE_BUTTON_PRESS:
			SingleButtonPressPacket singleButtonPressPacket = new SingleButtonPressPacket();
			singleButtonPressPacket.populateData(byteBuffer);
			listener.requestSingleButtonPress(this, singleButtonPressPacket);
			break;
		case SEQUENCE_BUTTON_PRESS:
			SequenceButtonPressPacket sequenceButtonPressPacket = new SequenceButtonPressPacket();
			sequenceButtonPressPacket.populateData(byteBuffer);
			listener.requestSequenceButtonPress(this, sequenceButtonPressPacket);
			break;
		case KEYBOARD_INPUT:
			KeyboardInputPacket keyboardInputPacket = new KeyboardInputPacket();
			keyboardInputPacket.populateData(byteBuffer);
			listener.requestKeyboardInput(this, keyboardInputPacket);
			break;
		default:
			break;
		}
	}
	
	/**
	 * This method is called by the ReadWriteHandler for the async socket connection.
	 * It is used to add a newly recieved packet to the processing queue.
	 * @param byteBuffer
	 */
	public void AddPacket(ByteBuffer byteBuffer) {
		try {
			if(byteBuffer.getInt(0) == 10) { handlePacket(byteBuffer); return; }
			accquire();
			recievedPackets.add(byteBuffer);
			release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Accquire the packet queue sempahore
	 * @throws InterruptedException
	 */
	private void accquire() throws InterruptedException {
		available.acquire();
	}
	
	/**
	 * Release the packet queue semaphore
	 */
	private void release() {
		available.release();
	}

	/**
	 * Get the game lobbies that currently have games instances running
	 * in them for a username.
	 * If the call succeeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void getGameLobbiesForUsername(String username) {
		GameLobbiesPacket packet = new GameLobbiesPacket(username);
		SendPacket(packet, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				// TODO Auto-generated method stub
				
			}
	
			
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
				
			}	
		}, null);	
	}

	/**
	 * Get the available teams for a given game lobby.
	 * If the call succeeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void getTeamsForGameLobby(int gameInstanceId, int gameLobbyId, String username) {
		GameTeamsPacket gameTeamsPacket = new GameTeamsPacket(gameInstanceId, gameLobbyId, username);
		SendPacket(gameTeamsPacket, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				//Packet sent successfully
			}

			
			public void failed(Throwable exc, Void attachment) {
				//Error sending packet
				exc.getMessage();
			}	
		}, null);
	}

	/**
	 * Joins a game lobby.
	 * If the call suceeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void joinGameLobby(int gameInstanceId, int gameLobbyId, byte team, String username) {
		ConnectPacket connectPacket = new ConnectPacket(gameInstanceId, username, gameLobbyId, team);
		SendPacket(connectPacket, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				// TODO Auto-generated method stub
				
			}
			
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		}, null);	
	}

	/**
	 * Sends a single button press in the format of 1-4 (red, green, blue, black).
	 * If the call suceeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void sendSingleButtonPress(int gameInstanceId, int team, int player, int buttonPress) {
		SingleButtonPressPacket singleButtonPressPacket = new SingleButtonPressPacket(gameInstanceId, team, player, buttonPress);
		SendPacket(singleButtonPressPacket, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				// TODO Auto-generated method stub
				
			}
			
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		}, null);
	}

	/**
	 * Sends a sequence button press in the format of 1234 (red, green, blue, black).
	 * If the call suceeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void sendSequenceButtonPress(int gameInstanceId, int team, int player, String sequenceButtonPress) {
		SequenceButtonPressPacket sequenceButtonPressPacket = new SequenceButtonPressPacket(gameInstanceId, team, player, sequenceButtonPress);
		SendPacket(sequenceButtonPressPacket, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				// TODO Auto-generated method stub
				
			}
			
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		}, null);
	}

	/**
	 * Sends keyboard input (text).
	 * If the call suceeds the completed method of your completion handler will be called.
	 * If the call fails the failed method will be called.
	 */
	public void sendKeyboardInput(int gameInstanceId, int team, int player, String keyboardInput) {
		KeyboardInputPacket keyboardInputPacket = new KeyboardInputPacket(gameInstanceId, team, player, keyboardInput);
		SendPacket(keyboardInputPacket, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				// TODO Auto-generated method stub
				
			}
			
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		}, null);
	}
}

/**
 * Internal class used by the async sockets for when reading of the
 * socket needs to be handled.
 * @author Matthew Micciolo
 *
 */
class ReadWriteHandler implements CompletionHandler<Integer, Server> {

	public void completed(Integer result, Server server) {
		
	    //Flip the buffer
	    server.buffer.flip();
	  
	    //Add all of the new bytes to the linked list of bytes
	    for(int i = 0; i < result; i++) {
		    server.inputBytes.add(server.buffer.get());
	    }
	  
	    //Clear the buffer so more data can be put into it
	    server.buffer.clear();	
	    
		//We need to read until all bytes of the packet have been returned
		//If we do not do this, full packets wont be read and data will get corrupt
	    while(true) {
	    	
			//If we have atleast 5 bytes (type + size)
		    if(server.inputBytes.size() >= 5 && server.packetLength == 0) {
			    byte[] bytes = {server.inputBytes.get(1), server.inputBytes.get(2), server.inputBytes.get(3), server.inputBytes.get(4)};
			    server.packetLength = ByteBuffer.wrap(bytes).getInt();
		    }
		    
		    //If we have enough bytes to finish processing a packet
			if(server.inputBytes.size() >= server.packetLength && server.packetLength != 0) {
				ByteBuffer buffer = ByteBuffer.allocate(server.packetLength);
				for(int i = 0; i < server.packetLength; i++) {
					buffer.put(server.inputBytes.removeFirst());
				}
				server.wlcpGameServer.AddPacket(buffer);
				server.packetLength = 0;
				if(server.inputBytes.size() == 0) {
					server.serverSocket.read(server.buffer, server, this); 
					break; 
				} 
			} else {
				if(server.packetLength != 0 ) {server.packetLength -= result;}
				server.serverSocket.read(server.buffer, server, this);
				break;
			}
	    }
	}

	public void failed(Throwable exc, Server attachment) {
		// TODO Auto-generated method stub
		
	}
	
}

/**
 * Internal class used to hold information about the server we are connected to.
 * @author Matthew Micciolo
 *
 */
class Server {
	
	public AsynchronousSocketChannel serverSocket;
	public ByteBuffer buffer = ByteBuffer.allocate(65535);
	public WLCPGameServer wlcpGameServer;
	public LinkedList<Byte> inputBytes = new LinkedList<Byte>();
	public int packetLength = 0;

	public Server(AsynchronousSocketChannel serverSocket, WLCPGameServer wlcpGameServer) {
		this.serverSocket = serverSocket;
		this.wlcpGameServer = wlcpGameServer;
	}
}