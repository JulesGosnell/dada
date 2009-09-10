package org.omo.cash;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Model;
import org.omo.core.TableModelView;
import org.omo.core.View;
import org.omo.core.TableModelView.Mapper;
import org.omo.jms.RemotingFactory;
import org.omo.old.demo.JView;

public class Client<K, V> {
	
	private final static Log LOG = LogFactory.getLog(Client.class);

	private final Mapper<K, V> mapper = new Mapper<K, V>() {

		@Override
		public Object getField(V value, int index) {
			switch (index) {
			case 0: return value;
			}
			throw new IllegalArgumentException("index out of bounds: " + index);
		}

		@Override
		public List<String> getFieldNames() {
			List<String> names = new ArrayList<String>();
			names.add("name");
			return names; // TODO: avoid reallocation...
		}

		@Override
		public K getKey(V value) {
			return (K)value; // TODO
		}
	};
	
	private final String serverName;
	private final String modelName;
	private final Session session;
	private final int timeout;
	private final boolean topLevel;
	private final TableModelView<K, V> guiModel;
	private final Destination serverDestination;
	private final RemotingFactory<Model<K, V>> clientFactory;
	private final Model<K, V> serverProxy;
	private final Destination clientDestination;
	private final RemotingFactory<View<K, V>> serverFactory;
	private final View<K, V> clientServer;
	private final JView jview;
	private final JTable table;
	private final JFrame frame;
	private final JPanel panel;

	private int selected  = -1;

	public Client(String serverName, String modelName, Session session, int timeout, boolean topLevel) throws JMSException {
		this.serverName = serverName;
		this.modelName = modelName;
		this.session = session;
		this.timeout = timeout;
		this.topLevel = topLevel;

		guiModel = new TableModelView<K, V>(mapper);

		serverDestination = session.createQueue(serverName + "." + modelName);
		clientFactory = new RemotingFactory<Model<K, V>>(session, Model.class, serverDestination, timeout);
		serverProxy = clientFactory.createSynchronousClient();

		// create a Client

		clientDestination = session.createQueue("Client."+ new UID().toString());
		serverFactory = new RemotingFactory<View<K, V>>(session, View.class, clientDestination, timeout);
		serverFactory.createServer(guiModel);
		clientServer = serverFactory.createSynchronousClient();

		// pass the client over to the server to attach as a listener..
		Collection<V> models = serverProxy.registerView(clientServer);
		if (models != null) guiModel.upsert(models);
		LOG.info("Client ready: "+clientDestination);

		jview = new JView(guiModel);
		table = jview.getTable();
		
		ListSelectionModel selectionModel = table.getSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int row = table.getSelectedRow();
					LOG.trace("SELECTION CHANGED: "+row);
					selected = row;
				}
			}
		});
		table.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String modelName = (String)guiModel.getValueAt(selected, 0);
					LOG.info("DOUBLE CLICK: "+modelName);
					try {
						new Client<String, String>(Client.this.serverName, modelName, Client.this.session, Client.this.timeout, false);
					} catch (JMSException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		frame = new JFrame(modelName);
		panel = new JPanel();
		new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.add(jview);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				serverProxy.deregisterView(clientServer);
				if (Client.this.topLevel)
					System.exit(0);
			}
		});
		frame.setVisible(true);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		final String serverName = (args.length == 0 ? "Server" : args[0]);
		String url = "peer://" + serverName + "/broker0?broker.persistent=false&useJmx=false";
		LOG.info("Broker URL: " +url);
		final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
		final Connection connection = connectionFactory.createConnection();
		connection.start();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		SwingUtilities.invokeAndWait(new Runnable() {
			
			@Override
			public void run() {
				try {
					new Client<String, String>(serverName, "MetaModel", session, 60000, true);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		// tidy up...
	}

}
