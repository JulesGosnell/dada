package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omo.core.Feed;
import org.omo.core.FilteredModelView;
import org.omo.core.IntrospectiveMetadata;
import org.omo.core.MapModelView;
import org.omo.core.Metadata;
import org.omo.core.Model;
import org.omo.core.Partitioner;
import org.omo.core.Filter;
import org.omo.core.StringMetadata;
import org.omo.core.View;
import org.omo.core.MapModelView.Adaptor;
import org.omo.jms.RemotingFactory;

public class Server {

	private final static Log LOG = LogFactory.getLog(Server.class);

	private static final String META_MODEL = "MetaModel";
	private final int timeout=60000;
	private final Connection connection;
	private final Session session;
	private final RemotingFactory<Model<String, String>> factory;
	private final MapModelView<String, String> metaModel;
	private final Adaptor<String, String> adaptor = new  Adaptor<String, String>() {@Override public String getKey(String value) {return value;}};
	
	public Server(String name, ConnectionFactory connectionFactory) throws JMSException, SecurityException, NoSuchMethodException {
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		factory = new RemotingFactory<Model<String, String>>(session, Model.class, (Destination)null, timeout);
		Metadata<String,String> modelMetadata = new StringMetadata("Name");
		metaModel = new MapModelView<String, String>(META_MODEL, modelMetadata, adaptor);
		Queue queue = session.createQueue(name + "." + META_MODEL);
		factory.createServer(metaModel, queue);
		metaModel.update(Collections.singleton(metaModel.getName())); // The metaModel is a Model !
		LOG.info("Listening on: " + queue);

		// we'' randomize trade dates out over the next week...
		final int DAY = 1000*60*60*24;
		final long now = new Date().getTime();
		final int numTrades = 100;
		final int numPartitions = 2;
		final int numDays = 5;
		final int numAccounts = 5;
		final int numCurrencies = 3;

		// adding TradeFeed
		IntrospectiveMetadata<Integer, Trade> tradeMetadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		Model<Integer, Trade> tradeFeed;
		{
			String feedName = "TradeFeed";
			Model<Integer, Trade> feed = new Feed<Integer, Trade>(feedName, tradeMetadata, numTrades, 100L, new Feed.Strategy<Integer, Trade>(){

				@Override
				public Trade createNewItem(int counter) {
					return new Trade(counter, 0, new Date(now + (long)(DAY * Math.random() * numDays)), new BigDecimal(100), (int)(Math.random()*numAccounts), (int)(Math.random()*numCurrencies));
				}

				@Override
				public Trade createNewVersion(Trade original) {
					return new Trade(original.getId(), original.getVersion()+1, original.getValueDate(), original.getAmount(), original.getAccount(), original.getCurrency());
				}

				@Override
				public Integer getKey(Trade item) {
					return item.getId();
				}});
			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName));
			feed.start();
			metaModel.update(Collections.singleton(feedName));
			tradeFeed = feed;
		}
		{
			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
			List<View<Integer, Trade>> partitions = new ArrayList<View<Integer, Trade>>();
			MapModelView.Adaptor<Integer, Trade> adaptor = new MapModelView.Adaptor<Integer, Trade>() {
				@Override
				public Integer getKey(Trade value) {
					return value.getId();
				}};
			for (int p=0; p<numPartitions; p++) {
				String partitionName = "Trade."+p;
				//MapModelView<Integer, Trade> partition = new MapModelView<Integer, Trade>(partitionName, tradeMetadata, adaptor);
				Filter filter = new Filter() {

					@Override
					public boolean apply(Object element) {
						return true;
					}

					@Override
					public LinkedList apply(Collection elements) {
						throw new UnsupportedOperationException("NYI");
					}
					
				};
				FilteredModelView<Integer, Trade> partition = new FilteredModelView<Integer, Trade>(partitionName, tradeMetadata, filter);
				partitions.add(partition);
				serverFactory.createServer(partition, session.createQueue("Server."+partitionName));
				partition.start();
				metaModel.update(Collections.singleton(partitionName));

				List<View<Integer, Trade>> days= new ArrayList<View<Integer, Trade>>();
				for (int d=0; d<numDays; d++) {
					String dayName = partitionName+".Date."+d;
					MapModelView<Integer, Trade> day = new MapModelView<Integer, Trade>(dayName, tradeMetadata, adaptor);
					days.add(day);
					serverFactory.createServer(day, session.createQueue("Server."+dayName));
					day.start();
					metaModel.update(Collections.singleton(dayName));

					List<View<Integer, Trade>> accounts= new ArrayList<View<Integer, Trade>>();
					for (int a=0; a<numAccounts; a++) {
						String accountName = dayName+".Account."+a;
						MapModelView<Integer, Trade> account= new MapModelView<Integer, Trade>(accountName, tradeMetadata, adaptor);
						accounts.add(account);
						serverFactory.createServer(account, session.createQueue("Server."+accountName));
						account.start();
						metaModel.update(Collections.singleton(accountName));
					}
					Partitioner<Integer, Trade> partitionerByAccount= new Partitioner<Integer, Trade>(accounts, new Partitioner.Strategy<Trade>() {

						@Override
						public int getNumberOfPartitions() {
							return numAccounts;
						}

						@Override
						public int partition(Trade value) {
							return value.getAccount();
						}});
					day.registerView(partitionerByAccount);

					List<View<Integer, Trade>> currencys= new ArrayList<View<Integer, Trade>>();
					for (int a=0; a<numCurrencies; a++) {
						String currencyName = dayName+".Currency."+a;
						MapModelView<Integer, Trade> currency= new MapModelView<Integer, Trade>(currencyName, tradeMetadata, adaptor);
						currencys.add(currency);
						serverFactory.createServer(currency, session.createQueue("Server."+currencyName));
						currency.start();
						metaModel.update(Collections.singleton(currencyName));
					}
					Partitioner<Integer, Trade> partitionerByCurrency= new Partitioner<Integer, Trade>(currencys, new Partitioner.Strategy<Trade>() {

						@Override
						public int getNumberOfPartitions() {
							return numCurrencies;
						}

						@Override
						public int partition(Trade value) {
							return value.getCurrency();
						}});
					day.registerView(partitionerByCurrency);

					{
					}
				}
				Partitioner<Integer, Trade> partitionerByDay = new Partitioner<Integer, Trade>(days, new Partitioner.Strategy<Trade>() {

					@Override
					public int getNumberOfPartitions() {
						return numDays;
					}

					@Override
					public int partition(Trade value) {
						return (int)((value.getValueDate().getTime() - now) / DAY) ;
					}});
				partition.registerView(partitionerByDay);

			}
			Partitioner<Integer, Trade> partitioner = new Partitioner<Integer, Trade>(partitions, new Partitioner.Strategy<Trade>() {

				@Override
				public int getNumberOfPartitions() {
					return numPartitions;
				}

				@Override
				public int partition(Trade value) {
					return value.getId() % numPartitions;
				}
			});
			tradeFeed.registerView(partitioner);
		}
		// adding AccountFeed
		{
			String feedName = "AccountFeed";
			IntrospectiveMetadata<Integer, Account> metadata = new IntrospectiveMetadata<Integer, Account>(Account.class, "Id");
			Feed.Strategy<Integer, Account> strategy = new Feed.Strategy<Integer, Account>(){
			@Override
			public Account createNewItem(int counter) {
				return new Account(counter, 0);
			}

			@Override
			public Account createNewVersion(Account original) {
				return new Account(original.getId(), original.getVersion()+1);
			}

			@Override
			public Integer getKey(Account item) {
				return item.getId();
			}};
			Model<Integer, Account> feed = new Feed<Integer, Account>(feedName, metadata, numAccounts, 100L, strategy);
			RemotingFactory<Model<Integer, Account>> serverFactory = new RemotingFactory<Model<Integer, Account>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName));
			feed.start();
			metaModel.update(Collections.singleton(feedName));
		}
		// adding CurrencyFeed
		{
			String feedName = "CurrencyFeed";
			Feed.Strategy<Integer, Currency> strategy = new Feed.Strategy<Integer, Currency>(){

				@Override
				public Currency createNewItem(int counter) {
					return new Currency(counter, 0);
				}

				@Override
				public Currency createNewVersion(Currency original) {
					return new Currency(original.getId(), original.getVersion()+1);
				}

				@Override
				public Integer getKey(Currency item) {
					return item.getId();
				}};
			Metadata<Integer, Currency> metadata = new IntrospectiveMetadata<Integer, Currency>(Currency.class, "Id");
			Model<Integer, Currency> feed = new Feed<Integer, Currency>(feedName, metadata, numCurrencies, 100L, strategy);
			RemotingFactory<Model<Integer, Currency>> serverFactory = new RemotingFactory<Model<Integer, Currency>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName));
			feed.start();
			metaModel.update(Collections.singleton(feedName));
		}
		// adding BalanceFeed
		{
			String feedName = "BalanceFeed";
			Feed.Strategy<Integer, Balance> strategy = new Feed.Strategy<Integer, Balance>(){

				@Override
				public Balance createNewItem(int counter) {
					return new Balance(counter, 0);
				}

				@Override
				public Balance createNewVersion(Balance original) {
					return new Balance(original.getId(), original.getVersion()+1);
				}

				@Override
				public Integer getKey(Balance item) {
					return item.getId();
				}};
			Metadata<Integer, Balance> metadata = new IntrospectiveMetadata<Integer, Balance>(Balance.class, "Id");
			Model<Integer, Balance> feed = new Feed<Integer, Balance>(feedName, metadata, 1000, 100L, strategy);
			RemotingFactory<Model<Integer, Balance>> serverFactory = new RemotingFactory<Model<Integer, Balance>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName));
			feed.start();
			metaModel.update(Collections.singleton(feedName));
		}
		// adding CompanyFeed
		{
			String feedName = "CompanyFeed";
			Feed.Strategy<Integer, Company> strategy = new Feed.Strategy<Integer, Company>(){

				@Override
				public Company createNewItem(int counter) {
					return new Company(counter, 0);
				}

				@Override
				public Company createNewVersion(Company original) {
					return new Company(original.getId(), original.getVersion()+1);
				}

				@Override
				public Integer getKey(Company item) {
					return item.getId();
				}};
			Metadata<Integer, Company> metadata = new IntrospectiveMetadata<Integer, Company>(Company.class, "Id");
			Model<Integer, Company> feed = new Feed<Integer, Company>(feedName, metadata, 10, 100L, strategy);
			RemotingFactory<Model<Integer, Company>> serverFactory = new RemotingFactory<Model<Integer, Company>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName));
			feed.start();
			metaModel.update(Collections.singleton(feedName));
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String name = (args.length == 0 ? "Server" : args[0]);
		String url = "peer://" + name + "/broker0?broker.persistent=false&useJmx=false";
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
		LOG.info("Broker URL: " +url);
		new Server(name, connectionFactory);

		// keep going...
		while (true)
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

}
