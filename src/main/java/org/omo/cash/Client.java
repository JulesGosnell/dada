package org.omo.cash;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
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

public class Client {
	
	private final static Log LOG = LogFactory.getLog(Client.class);

	private final Mapper<String, String> mapper = new Mapper<String, String>() {

		@Override
		public Object getField(String value, int index) {
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
		public String getKey(String value) {
			return value;
		}
	};
	
	private final boolean topLevel;
	private final Connection connection;
	private final Session session;
	private final TableModelView<String, String> guiModel;
	private final Destination serverDestination;
	private final RemotingFactory<Model<String, String>> clientFactory;
	private final Model<String, String> serverProxy;
	private final Destination clientDestination;
	private final RemotingFactory<View<String, String>> serverFactory;
	private final View<String, String> clientServer;
	private final JView jview;
	private final JTable table;
	private final JFrame frame;
	private final JPanel panel;

	private int selected  = -1;

	public Client(String serverName, ConnectionFactory connectionFactory, int timeout, boolean topLevel) throws JMSException {
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		guiModel = new TableModelView<String, String>(mapper);
		this.topLevel = topLevel;

		serverDestination = session.createQueue(serverName + "." + "MetaModel");
		clientFactory = new RemotingFactory<Model<String, String>>(session, Model.class, serverDestination, timeout);
		serverProxy = clientFactory.createSynchronousClient();

		// create a Client

		clientDestination = session.createQueue("Client.all.Listener");
		serverFactory = new RemotingFactory<View<String, String>>(session, View.class, clientDestination, timeout);
		serverFactory.createServer(guiModel);
		clientServer = serverFactory.createSynchronousClient();

		// pass the client over to the server to attach as a listener..
		Collection<String> models = serverProxy.registerView(clientServer);
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
					
					LOG.info("DOUBLE CLICK: "+selected);
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
		
		frame = new JFrame("Client");
		panel = new JPanel();
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
		SwingUtilities.invokeAndWait(new Runnable() {
			
			@Override
			public void run() {
				try {
					new Client(serverName, connectionFactory, 60000, true);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		// tidy up...
	}

}
