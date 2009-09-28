package org.omo.cash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.omo.core.Filter;
import org.omo.core.FilteredModelView;
import org.omo.core.IdentityFilter;
import org.omo.core.IntrospectiveMetadata;
import org.omo.core.MapModelView;
import org.omo.core.Metadata;
import org.omo.core.Model;
import org.omo.core.Partitioner;
import org.omo.core.Registration;
import org.omo.core.StringMetadata;
import org.omo.core.View;
import org.omo.core.MapModelView.Adaptor;
import org.omo.jms.RemotingFactory;

public class Server {

	private final class TradeFeedStrategy implements Feed.Strategy<Integer, Trade> {

		private final Date startOfWeek;
		private final Date endOfWeek;
		private final long millisInWeek;
		private final int numAccounts;
		private final int numCurrencies;

		private TradeFeedStrategy(int numAccounts, int numCurrencies) {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			startOfWeek = calendar.getTime();

			calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 999);
			endOfWeek = calendar.getTime();

			millisInWeek = endOfWeek.getTime() - startOfWeek.getTime();

			this.numAccounts = numAccounts;
			this.numCurrencies = numCurrencies;
		}

		@Override
		public Trade createNewValue(int counter) {
			Date date = new Date(startOfWeek.getTime() + (long)(Math.random() * millisInWeek));
			return new Trade(counter, 0, date, new BigDecimal(100), (int)(Math.random()*numAccounts), (int)(Math.random()*numCurrencies));
		}

		@Override
		public Trade createNewVersion(Trade original) {
			return new Trade(original.getId(), original.getVersion()+1, original.getValueDate(), original.getAmount(), original.getAccount(), original.getCurrency());
		}

		@Override
		public Integer getKey(Trade item) {
			return item.getId();
		}
	}

	private final static Log LOG = LogFactory.getLog(Server.class);

	private static final String META_MODEL = "MetaModel";
	private final int timeout=60000;
	private final Connection connection;
	private final Session session;
	private final RemotingFactory<Model<String, String>> factory;
	private final MapModelView<String, String> metaModel;
	private final Adaptor<String, String> adaptor = new  Adaptor<String, String>() {@Override public String getKey(String value) {return value;}};
	private final Executor executor =  new ThreadPoolExecutor(10, 100, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));

	public Server(String name, ConnectionFactory connectionFactory) throws JMSException, SecurityException, NoSuchMethodException {
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		factory = new RemotingFactory<Model<String, String>>(session, Model.class, (Destination)null, timeout);
		Metadata<String,String> modelMetadata = new StringMetadata("Name");
		metaModel = new MapModelView<String, String>(META_MODEL, modelMetadata, adaptor);
		Queue queue = session.createQueue(name + "." + META_MODEL);
		factory.createServer(metaModel, queue, executor);
		metaModel.update(Collections.singleton(metaModel.getName())); // The metaModel is a Model !
		LOG.info("Listening on: " + queue);

		// we'' randomize trade dates out over the next week...
		final int numTrades = 10000;
		final int numPartitions = 3;
		final int numDays = 5;
		final int numAccounts = 5;
		final int numCurrencies = 2;

		// adding TradeFeed
		IntrospectiveMetadata<Integer, Trade> tradeMetadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		Model<Integer, Trade> tradeFeed;
		{
			String feedName = "TradeFeed";
			Model<Integer, Trade> feed = new Feed<Integer, Trade>(feedName, tradeMetadata, numTrades, 100L, new TradeFeedStrategy(numAccounts, numCurrencies));
			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
			serverFactory.createServer(feed, session.createQueue("Server."+feedName), executor);
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

				Map<String, FilteredModelView<Integer, Trade>> aggregatedAccounts = new HashMap<String, FilteredModelView<Integer,Trade>>();
				for (int p=0; p<numPartitions; p++) {
					String partitionName = "Trade."+p;
					//MapModelView<Integer, Trade> partition = new MapModelView<Integer, Trade>(partitionName, tradeMetadata, adaptor);
					Filter<Trade> filter = new Filter<Trade>() {

						@Override
						public boolean apply(Trade value) {
							return true;
						}
					};
					FilteredModelView<Integer, Trade> partition = new FilteredModelView<Integer, Trade>(partitionName, tradeMetadata, filter);
					partitions.add(partition);
					serverFactory.createServer(partition, session.createQueue("Server."+partitionName), executor);
					partition.start();
					metaModel.update(Collections.singleton(partitionName));

					Calendar calendar = Calendar.getInstance();
					calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);

					for (int d=0; d<numDays; d++) {
						String dayName = partitionName+".Date."+d;
						Date start = calendar.getTime();
						calendar.set(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK) + 1);
						Date end = calendar.getTime();
						Filter<Trade> valueDateFilter = new ValueDateFilter(start, end);
						FilteredModelView<Integer, Trade> day = new FilteredModelView<Integer, Trade>(dayName, tradeMetadata, valueDateFilter);
						view(partition, day);
						serverFactory.createServer(day, session.createQueue("Server."+dayName), executor);
						day.start();
						metaModel.update(Collections.singleton(dayName));

						for (int a=0; a<numAccounts; a++) {
							String accountName = dayName+".Account."+a;
							Filter<Trade> f = new AccountFilter(a);
							FilteredModelView<Integer, Trade> account= new FilteredModelView<Integer, Trade>(accountName, tradeMetadata, f);
							serverFactory.createServer(account, session.createQueue("Server."+accountName), executor);
							view(day, account);
							account.start();
							metaModel.update(Collections.singleton(accountName));

							if (p == 0) {
								String name2 = "Trade.Date."+d+".Account."+a;
								FilteredModelView<Integer, Trade> model = new FilteredModelView<Integer, Trade>(name2, tradeMetadata, new IdentityFilter<Trade>());
								serverFactory.createServer(model, session.createQueue("Server."+name2), executor);
								model.start();
								aggregatedAccounts.put(name2, model);
								metaModel.update(Collections.singleton(name2));
							}
							FilteredModelView<Integer, Trade> aggregateModel = aggregatedAccounts.get("Trade.Date."+d+".Account."+a);
							view(account, aggregateModel);
						}

						for (int c=0; c<numCurrencies; c++) {
							String currencyName = dayName+".Currency."+c;
							Filter<Trade> f = new CurrencyFilter(c);
							FilteredModelView<Integer, Trade> currency= new FilteredModelView<Integer, Trade>(currencyName, tradeMetadata, f);
							serverFactory.createServer(currency, session.createQueue("Server."+currencyName), executor);
							view(day, currency);
							currency.start();
							metaModel.update(Collections.singleton(currencyName));
						}
					}
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
				view(tradeFeed, partitioner);
		}

		// adding AccountFeed
		{
			String feedName = "AccountFeed";
			IntrospectiveMetadata<Integer, Account> metadata = new IntrospectiveMetadata<Integer, Account>(Account.class, "Id");
			Feed.Strategy<Integer, Account> strategy = new Feed.Strategy<Integer, Account>(){
				@Override
				public Account createNewValue(int counter) {
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
				serverFactory.createServer(feed, session.createQueue("Server."+feedName), executor);
				feed.start();
				metaModel.update(Collections.singleton(feedName));
		}
		// adding CurrencyFeed
		{
			String feedName = "CurrencyFeed";
			Feed.Strategy<Integer, Currency> strategy = new Feed.Strategy<Integer, Currency>(){

				@Override
				public Currency createNewValue(int counter) {
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
				serverFactory.createServer(feed, session.createQueue("Server."+feedName), executor);
				feed.start();
				metaModel.update(Collections.singleton(feedName));
		}
		// adding BalanceFeed
		{
			String feedName = "BalanceFeed";
			Feed.Strategy<Integer, Balance> strategy = new Feed.Strategy<Integer, Balance>(){

				@Override
				public Balance createNewValue(int counter) {
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
				serverFactory.createServer(feed, session.createQueue("Server."+feedName), executor);
				feed.start();
				metaModel.update(Collections.singleton(feedName));
		}
		// adding CompanyFeed
		{
			String feedName = "CompanyFeed";
			Feed.Strategy<Integer, Company> strategy = new Feed.Strategy<Integer, Company>(){

				@Override
				public Company createNewValue(int counter) {
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
				serverFactory.createServer(feed, session.createQueue("Server."+feedName), executor);
				feed.start();
				metaModel.update(Collections.singleton(feedName));
		}
	}
	
	protected void view(Model<Integer, Trade> model, View<Integer, Trade> view) {
		Registration<Integer, Trade> registration = model.registerView(view);
		view.update(registration.getData());
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
