package org.omo.cash;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.omo.core.JView;
import org.omo.core.Model;
import org.omo.core.Registration;
import org.omo.core.TableModelView;
import org.omo.core.Update;
import org.omo.core.View;
import org.omo.jms.RemotingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
	
	private final static Logger LOG = LoggerFactory.getLogger(Client.class);
	private final static ExecutorService executorService = Executors.newFixedThreadPool(20);

	private final String serverName;
	private final String modelName;
	private final Session session;
	private final int timeout;
	private final boolean topLevel;
	private final TableModelView<Object, Object> guiModel;
	private final Destination serverDestination;
	private final RemotingFactory<Model<Object, Object>> clientFactory;
	private final Model<Object, Object> serverProxy;
	private final Destination clientDestination;
	private final RemotingFactory<View<Object, Object>> serverFactory;
	private final View<Object, Object> clientServer;
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

		guiModel = new TableModelView<Object, Object>();
		LOG.info("viewing: " + this.modelName);

		serverDestination = session.createQueue(this.modelName);
		clientFactory = new RemotingFactory<Model<Object, Object>>(session, Model.class, timeout);
		serverProxy = clientFactory.createSynchronousClient(serverDestination, true);

		// create a Client

		clientDestination = session.createQueue("Client." + new UID().toString()); // tie up this UID with the one in RemotingFactory
		serverFactory = new RemotingFactory<View<Object, Object>>(session, View.class, timeout);
		serverFactory.createServer(guiModel, clientDestination, executorService);
		clientServer = serverFactory.createSynchronousClient(clientDestination, true);

		// pass the client over to the server to attach as a listener..
		Registration<Object, Object> registration = serverProxy.registerView(clientServer); 
		Collection<Object> models = registration.getData();
		if (models != null) {
			guiModel.setMetadata(registration.getMetadata());
			Collection<Update<Object>> insertions = new ArrayList<Update<Object>>();
			for (Object model : models)
				insertions.add(new Update<Object>(null ,model));
			guiModel.update(insertions, new ArrayList<Update<Object>>(), new ArrayList<Update<Object>>());
		}
		else
			LOG.warn("null model content returned");
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
					String targetModelName = (String)guiModel.getValueAt(selected, 0);
					LOG.info("Opening: "+targetModelName);
					try {
						new Client(Client.this.serverName, targetModelName, Client.this.session, Client.this.timeout, false);
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
		
		frame = new JFrame(this.modelName);
		panel = new JPanel();
		new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.add(jview);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		frame.pack();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				LOG.info("Closing: "+Client.this.modelName);
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
		//String url = "peer://" + serverName + "/broker0?broker.persistent=false&useJmx=false";
		String url = "tcp://localhost:61616";
		LOG.info("Broker URL: " +url);
		final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
		final Connection connection = connectionFactory.createConnection();
		connection.start();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		SwingUtilities.invokeAndWait(new Runnable() {
			
			@Override
			public void run() {
				try {
					new Client(serverName, serverName+".MetaModel", session, 60000, true);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		// tidy up...
	}

}
