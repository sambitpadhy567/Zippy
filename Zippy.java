import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import javax.sound.sampled.*;

public class Zippy{
	public static void main(String[] args){
		System.out.println("Application started");
		Connection conn = new Connection();
		//new ChooseMode(null, "client", null, null);
		//new TextChat();
		//new ChatView();
		//new Fil`(null, null, null, null);
	}
}

class Connection extends JFrame{
	private JButton server_btn;
	private JButton client_btn;
	private JTextField ip_txt;
	private String mode;
	private Server server_sock;
	private Client clinet_sock;

	public Connection(){
		initUI();
	}
	protected void initUI(){
		getContentPane().removeAll();
		setTitle("Connect");
        setSize(250, 120);
        getContentPane().setLayout(new FlowLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ip_txt = new JTextField(9);
        ip_txt.setSize(10, 50);
        //ip.setP
        add(ip_txt);
        client_btn = new JButton("Send");
        add(client_btn);
        client_btn.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		mode = "client";
        		createSocket();
        		setVisible(false);
        		//dispose();
        	}
        });
        server_btn = new JButton("Listen");
        add(server_btn);
        server_btn.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		mode = "server";
        		createSocket();
        		setVisible(false);
        		//dispose();
        	}
        });
        setResizable(false);
        setVisible(true);
	}
	private void createSocket(){
		if(mode.equals("server")){
			try{
				server_sock = new Server();
				//add(new JLabel("Connection successful"));
				new ChooseMode(this, mode, server_sock, clinet_sock);
			} catch(Exception e){
				System.out.println("createSocket server: "+e.toString());
			}
		} else if(mode.equals("client")){
			try{
				String IP = ip_txt.getText();
				clinet_sock = new Client(IP);
				//add(new JLabel("Connection successful"));
				new ChooseMode(this, mode, server_sock, clinet_sock);
			} catch(Exception e){
				System.out.println("createSocket client: "+e.toString());
			}
		}
	}
}

class Server{
	private final int PORT = 12345;
	private ServerSocket server_sock;
	private Socket socket;

	public Server() throws IOException{
		try{
			server_sock = new ServerSocket(PORT);
			System.out.println("Server socket created.\nWaiting for connection at PORT: "+PORT);
			socket = server_sock.accept();
			System.out.println("Connection accepted from "+socket.getRemoteSocketAddress());
		} catch(Exception e){
			System.out.println("Server(): "+e.toString());
		}
	}
	public DataInputStream getInputStream() throws IOException{
		return new DataInputStream(socket.getInputStream());
	}
	public DataOutputStream getOutputStream() throws IOException{
		return new DataOutputStream(socket.getOutputStream());
	}
	public void closeSocket() throws IOException{
		server_sock.close();
		System.out.println("Socket closed Successfully");
	}
}

class Client{
	private final int PORT = 12345;
	Socket socket;

	Client(String ip) throws IOException{
		System.out.println("Connecting to " + ip + ":" + PORT);
		socket = new Socket(ip, PORT);
		System.out.println("Connection successful with " + socket.getRemoteSocketAddress());
	}
	public DataInputStream getInputStream() throws IOException{
		return new DataInputStream(socket.getInputStream());	
	}
	public DataOutputStream getOutputStream() throws IOException{
		return new DataOutputStream(socket.getOutputStream());
	}
	public void closeSocket() throws IOException{
		socket.close();
		System.out.println("Socket closed Successfully");
	}
}

class ChooseMode extends JFrame implements ActionListener{
	private final String mode;
	private JButton text_btn;
	private JButton voice_btn;
	private JButton video_btn;
	private JButton back_btn;
	private final Server server;
	private final Client client;
	private final Connection parent;
	private volatile boolean flag_sent = false;
	private volatile boolean flag_received = false;
	private DataInputStream in;
	private DataOutputStream out;
	private ChooseMode self;

	public ChooseMode(Connection parent, String mode, Server server, Client client){
		this.mode = mode;
		this.server = server;
		this.client = client;
		this.parent = parent;
		self = this;
		try{
			if(mode.equals("server")){
				in = server.getInputStream();
				out = server.getOutputStream();
			} else if(mode.equals("client")){
				in = client.getInputStream();
				out = client.getOutputStream();
			}
		} catch(Exception e){
			System.out.println("VoiceCall: "+e.toString());
		}
		initUI();
		//waitFor();
	}
	protected void initUI(){
		getContentPane().removeAll();
		setTitle("Choose");
        setSize(180, 180);
        getContentPane().setLayout(new FlowLayout());
        text_btn = new JButton("Text Chat");
        add(text_btn);
        text_btn.addActionListener(this);
        voice_btn = new JButton("Audio Call");
        add(voice_btn);
        voice_btn.addActionListener(this);
        video_btn = new JButton("File Share");
        add(video_btn);
        video_btn.addActionListener(this);
        back_btn = new JButton("Disconnect");
        add(back_btn);
        back_btn.addActionListener(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
        waitFor();
	}
	private void waitFor(){
		flag_sent = false;
		flag_received = false;
		new Thread(new Runnable(){
			public void run(){
				try{
					System.out.println("Waiting: ");
					String tmp = in.readUTF();
					while(!flag_sent && !(tmp.equals("*#*T") || tmp.equals("*#*A") || tmp.equals("*#*V") || tmp.equals("*#*Q")))
						tmp = in.readUTF();
					System.out.println("Received: "+tmp+" "+flag_sent+" "+flag_received);
					if(!flag_sent){
						flag_sent = true;
						flag_received = true;
						out.writeUTF(tmp);
						setVisible(false);
						if(tmp.equals("*#*T"))
							new TextChat(self, mode, server, client);
						else if(tmp.equals("*#*A"))
							new VoiceCall(self, mode, server, client);
						else if(tmp.equals("*#*V"))
							new FileTransfer(self, mode, server, client);
						else{
							out.close();
							in.close();
							parent.initUI();
							dispose();
						}
					}
					else
						flag_received = true;
				} catch(Exception e){
					System.out.println("actionPerformed Thread: "+e.toString());
				}
			}
		}).start();
	}
	public void actionPerformed(ActionEvent e){
		try{
			if(e.getSource() == text_btn){
				setVisible(false);
				flag_sent = true;
				System.out.println("clicked: "+flag_received);
				out.writeUTF("*#*T");
				while(!flag_received);//{System.out.println(flag_received);}
				new TextChat(this, mode, server, client);
				System.out.println("TextChat created");
			} else if(e.getSource() == voice_btn){
				setVisible(false);
				flag_sent = true;
				System.out.println("clicked: "+flag_received);
				out.writeUTF("*#*A");
				while(!flag_received);//{i+=i;}//System.out.println(flag_received);}
				new VoiceCall(this, mode, server, client);
				System.out.println("VoiceCall created");
			} else if(e.getSource() == video_btn){
				setVisible(false);
				flag_sent = true;
				System.out.println("clicked: "+flag_received);
				out.writeUTF("*#*V");
				while(!flag_received);//{System.out.println(flag_received);}
				new FileTransfer(this, mode, server, client);
				System.out.println("FileTransfer created");
			} else if(e.getSource() == back_btn){
				out.close();
				in.close();
				parent.initUI();
				dispose();
			}
		} catch(Exception ex){
			System.out.println("ChooseMode actionPerformed: "+ex.toString());
		}
	}
}

class Chat extends JFrame{
	protected final String mode;
	protected final Server server;
	protected final Client client;
	protected final ChooseMode parent;
	protected DataInputStream in = null;
	protected DataOutputStream out = null;
	protected volatile boolean isCalling = false;

	public Chat(ChooseMode parent, String mode, Server server, Client client){
		this.mode  = mode;
		this.server = server;
		this.client = client;
		this.parent = parent;
		try{
			if(mode.equals("server")){
				in = server.getInputStream();
				out = server.getOutputStream();
			} else if(mode.equals("client")){
				in = client.getInputStream();
				out = client.getOutputStream();
			}
		} catch(Exception e){
			System.out.println("Chat: "+e.toString());
		}
	}
}

class TextChat extends Chat implements ActionListener{
	private JButton send_btn;
	private JButton back_btn;
	private ChatView msg_box;
	private JTextArea input;
	private byte[] data = new byte[5000];

	public TextChat(){
		super(null, null, null, null);
		initUI();
	}
	public TextChat(ChooseMode parent, String mode, Server server, Client client){
		super(parent, mode, server, client);
		initUI();
	}
	protected void initUI(){
		isCalling = true;
		new Thread(new Runnable(){
			public void run(){
				receiveMsg();
			}
		}).start();
		getContentPane().removeAll();
		setLayout(new BorderLayout());
		msg_box = new ChatView();
		

		JScrollPane scroll = new JScrollPane(new Label("Scroll View"));
		scroll.setViewportView(msg_box);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//scroll.setViewportBorder(BorderFactory.createLineBorder(Color.RED));
		//add(scroll, BorderLayout.CENTER);
		//msg_box.addLabel("label 1", 'l');
		//msg_box.addLabel("label 1", 'r');

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		input = new JTextArea();
		input.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent ev){
				if(ev.getKeyCode() == KeyEvent.VK_ENTER && !ev.isShiftDown()){
					ev.consume();
					send_btn.doClick();
				}  else if(ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isShiftDown()){
					ev.consume();
					input.append("\n");
				}
			}
		});
		send_btn = new JButton("Send");
		send_btn.addActionListener(this);
		panel.add(input);
		back_btn = new JButton("Back");
		back_btn.addActionListener(this);
		panel.add(back_btn);
		panel.add(send_btn);
		//add(panel, BorderLayout.SOUTH);
		setSize(400, 500);
		setTitle("Text Chat");
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, panel);
		split.setDividerLocation(400);
		add(split);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
		System.out.println("TextChat object");
	}

	private void receiveMsg(){
		System.out.println("Receive started");
		try{
			while(isCalling){
				String msg = in.readUTF();
				System.out.println("Received: "+msg);
				if(msg.equals("*#*Q")){
					isCalling = false;
					parent.initUI();
					dispose();
					break;
				}
				msg_box.addLabel(msg, 'l');
				input.requestFocusInWindow();
			}
		} catch(Exception e){
			System.out.println("receiveMsg: "+e.toString());
		}
	}

	public void actionPerformed(ActionEvent ev){
		if(ev.getSource() == send_btn){
			sendMsg(input.getText().toString());
		} else if(ev.getSource() == back_btn){
			sendMsg("*#*Q");
			isCalling = false;
			parent.initUI();
			dispose();
		}
	}

	private void sendMsg(String msg){
		try{
			//String msg = input.getText().toString();
			if(msg.length() == 0)
				return;
			input.setText("");
			out.writeUTF(msg);
			System.out.println("Sent: "+msg);
			msg_box.addLabel(msg, 'r');
			input.requestFocusInWindow();
		} catch(Exception e){
			System.out.println("actionPerformed: "+e.toString());
		}
	}
}

class ChatView extends JPanel{

	public ChatView(){
		//setSize(500, 600);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setSize(400, 500);
		//setMargin(new Insets(0, 40, 0, 40));
		setVisible(true);
	}

	public void addLabel(String txt, char orient){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				System.out.println("addLabel called");
				JLabel label;
				if(orient == 'l'){
					label = new JLabel("<html><body style=\"width: 300px; text-align: left; margin-left: 10px;\">"+ txt +"</body></html>", SwingConstants.LEFT);
					//label.setMargin(new Insets(0, 40, 0, 0));
				}
				else if(orient == 'r'){
					label = new JLabel("<html><body style=\"width: 300px; text-align: right; margin-right: 20px\">"+ txt +"</body></html>", SwingConstants.RIGHT);
					//label.setMargin(new Insets(0, 0, 0, 40));
				}
				else
					label = null;
				//label.setHorizontalAlignment(SwingConstants.RIGHT);
				//label.setBorder(BorderFactory.createLineBorder(Color.RED));
				label.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
				add(label);
				validate();
			}
		});
	}
}

class VoiceCall extends Chat implements ActionListener{
	private JButton start_call;
	private JButton end_call;	
	private volatile boolean flag = false;

	public VoiceCall(ChooseMode parent, String mode, Server server, Client client){
		super(parent, mode, server, client);
		try{
			if(mode.equals("server")){
				in = server.getInputStream();
				out = server.getOutputStream();
			} else if(mode.equals("client")){
				in = client.getInputStream();
				out = client.getOutputStream();
			}
			initUI();
		} catch(Exception e){
			System.out.println("VoiceCall: "+e.toString());
		}
	}
	protected void initUI(){
		new Thread(new Runnable(){
				public void run(){
					try{
						byte[] tmp = new byte[5];
						in.read(tmp);
						isCalling = true;
						if(!flag){
							out.write("*#*S".getBytes());
							recordAudio();
							playAudio();
						}
					} catch(Exception e){
						System.out.println("VoiceCall Thread: "+e.toString());
					}
				}
			}).start();
		getContentPane().removeAll();
		System.out.println("VoiceCall object");
		setTitle("Voice Call");
        setSize(200, 150);
        getContentPane().setLayout(new FlowLayout());
        start_call = new JButton("Start");
        add(start_call);
        start_call.addActionListener(this);
        end_call = new JButton("End");
        add(end_call);
        end_call.addActionListener(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
	}
	public void actionPerformed(ActionEvent e){
		try{
			if(e.getSource() == start_call){
				flag = true;
				out.write("*#*S".getBytes());
				while(!isCalling);
				recordAudio();
				playAudio();
			} else if(e.getSource() == end_call){
				isCalling = false;
				parent.initUI();
				dispose();
			}
		} catch(Exception ex){
			System.out.println("actionPerformed: "+e.toString());
		}
	}
	private AudioFormat getFormat() {
		float sampleRate = 8000;
		int sampleSizeInBits = 8;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}
	private void recordAudio(){
		new Thread(new Runnable(){
			public void run(){
				try{
					//Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
					DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, getFormat());
					//Mixer mixer = AudioSystem.getMixer(mixerInfo[3]);
					//TargetDataLine line = (TargetDataLine)mixer.getLine(dataLineInfo);
					TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
					line.open();
					line.start();
					byte temp[] = new byte[3000];
					System.out.println("written " + temp.toString());
					while(isCalling){
						int c = line.read(temp, 0, temp.length);
						out.write(temp, 0, c);
						System.out.println("written " + temp.toString());
					}
					// System.out.println("Inside recordAudio isCalling:false");
					// out.write("*#*Q".getBytes());
				} catch(Exception e){
					System.out.println("recordAudio: "+e.toString());
				}
			}
		}).start();
		
	}
	private void playAudio(){
		new Thread(new Runnable(){
			public void run(){
				try{
					DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, getFormat());
					SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
					sourceDataLine.open(getFormat());
     				sourceDataLine.start();
     				byte temp[] = new byte[3000];
     				System.out.println("read " + temp.toString());
     				while(isCalling){
     					int cnt = in.read(temp, 0, temp.length);
     					if(temp.toString().equals("*#*Q"))
     						break;
     					if(cnt > 0)
     						sourceDataLine.write(temp,0,cnt);
     					System.out.println("Read " + temp.toString());
     				}
     				//Webcam w = new Webcam();
     				//Robot rt = new Robot();
     				//BufferedImage img = rt.createScreenCapture(new Rectangle(100, 200));
     				sourceDataLine.drain();
      				sourceDataLine.close();
      				// System.out.println("Inside playAudio isCalling:false");
      				// if(temp.toString().equals("*#*Q")){
      				// 	parent.initUI();
      				// 	dispose();
      				// }
      				//out.write("*#*Q".getBytes());
				} catch(Exception e){
					System.out.println("playAudio: "+e.toString());
				}
			}
		}).start();
	}
}

class FileTransfer extends Chat implements ActionListener{

	private JButton file_transfer;
	private JFileChooser fc;
	private JButton file_send;
	private JButton file_recv;
	private JLabel file_name;
	private JLabel file_progress;

	public FileTransfer(ChooseMode parent, String mode, Server server, Client client){
		super(parent, mode, server, client);
		
		initUI();
	}
	protected void initUI(){
		getContentPane().removeAll();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(200, 200);
		setLayout(new FlowLayout());
		file_transfer = new JButton("Choose file");
		add(file_transfer);
		file_send = new JButton("Send file");
		add(file_send);
		file_recv = new JButton("Receive");
		add(file_recv);
		file_name = new JLabel("No files choosen");
		add(file_name);
		file_progress = new JLabel();
		add(file_progress);
		file_transfer.addActionListener(this);
		file_send.addActionListener(this);
		file_recv.addActionListener(this);
		setVisible(true);
		fc = new JFileChooser();
		System.out.println("FileTransfer object");
	}

	public void actionPerformed(ActionEvent ev){
		try{
			if(ev.getSource() == file_transfer){
				System.out.println("Button clicked");
				fc.showOpenDialog(this);
				file_name.setText(fc.getSelectedFile().getName());
				System.out.println(fc.getSelectedFile());
			} else if(ev.getSource() == file_recv){
				file_recv.setEnabled(false);
				new Thread(new Runnable(){
					public void run(){
						File tmp = null;
						try{
							byte[] buffer = new byte[100000];
							String f_name = in.readUTF();
							long f_size = Long.parseLong(in.readUTF());
							System.out.println("Name: "+f_name+" Size: "+f_size);
							file_name.setText("Receiving: "+f_name+"("+f_size+"Bytes)");
							tmp = new File("Zippy/"+f_name);
							JFileChooser tf = new JFileChooser();
							tf.setSelectedFile(tmp);
							tf.showSaveDialog(getContentPane());
							//if(tf.showSaveDialog(getContentPane()) == JFileChooser.APPROVE_OPTION)
								//tmp.renameTo(new File(tf.getSelectedFile().toString()));
							long c = 0;
							int read = 0;
							//tmp = new File(tf.getSelectedFile().toString());
							//tmp = new File(f_name);
							//FileOutputStream file = new FileOutputStream(new File(f_name));
							FileOutputStream file = new FileOutputStream(tf.getSelectedFile().toString());
							BufferedOutputStream buf = new BufferedOutputStream(file);
							while(true){
								read = in.read(buffer);
								buf.write(buffer, 0, read);
								//file.write(buffer);
								c += read;
								//System.out.println(read+" "+c+" "+f_size);
								if(read <= 0 || c >= f_size){
									//System.out.println("Exiting: "+read);
									break;
								}
								file_progress.setText(String.valueOf(100<100*c/f_size?"100%":100*c/f_size+"%"));
							}
							file_progress.setText("100%");
							file_recv.setEnabled(true);
							System.out.println("receiving finished");
							if(Desktop.isDesktopSupported())
								Desktop.getDesktop().open(new File(tf.getCurrentDirectory().toString()));
							buf.close();
						} catch(Exception e){
							//if(tmp != null)
							//	tmp.delete();
							System.out.println("file receiving Thread"+e.toString());
						}
					}
				}).start();
			} else if(ev.getSource() == file_send){
				file_progress.setText("0%");
				file_send.setEnabled(false);
				file_name.setEnabled(false);
				file_transfer.setEnabled(false);
				file_send.setEnabled(false);
				new Thread(new Runnable(){
					public void run(){	
						try{
							long f_size = fc.getSelectedFile().length();
							String f_name = fc.getSelectedFile().getName();
							file_name.setText("Receiving: "+f_name+"("+f_size+"Bytes)");
							out.writeUTF(f_name);
							out.writeUTF(String.valueOf(f_size));
							System.out.println("Name: "+f_name+" Size: "+f_size);
							byte[] buffer = new byte[100000];
							FileInputStream fis = new FileInputStream(fc.getSelectedFile());
							BufferedInputStream buf = new BufferedInputStream(fis);
							long c = 0;
							int read = 0;
							while(true){
								//read = fis.read(buffer);
								read = buf.read(buffer);

								if(read <= 0){
									//out.write(null);
									break;
								}
								out.write(buffer, 0, read);
								c += read;
								//System.out.println(read+" "+c);
								file_progress.setText(String.valueOf(100<100*c/f_size?"100%":100*c/f_size+"%"));
							}
							file_progress.setText("100%");
							file_send.setEnabled(true);
							file_name.setEnabled(true);
							file_transfer.setEnabled(true);
							file_send.setEnabled(true);
							System.out.println("sending finished");
							buf.close();
						} catch(Exception e){
							System.out.println("file sending Thread"+e.toString());
						}
					}
				}).start();
				//File file = new File(fc.getSelectedFile().getName());
				
			}
		} catch(Exception e){
			System.out.println("File transfer: "+e.toString());
		}
	}
}