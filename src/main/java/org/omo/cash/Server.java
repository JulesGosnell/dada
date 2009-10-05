package org.omo.cash;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.omo.core.DateRange;
import org.omo.core.Feed;
import org.omo.core.Filter;
import org.omo.core.FilteredModelView;
import org.omo.core.IdentityFilter;
import org.omo.core.IntegerRange;
import org.omo.core.IntrospectiveMetadata;
import org.omo.core.MapModelView;
import org.omo.core.Metadata;
import org.omo.core.Model;
import org.omo.core.ModelView;
import org.omo.core.Partitioner;
import org.omo.core.Range;
import org.omo.core.Registration;
import org.omo.core.StringMetadata;
import org.omo.core.View;
import org.omo.core.MapModelView.Adaptor;
import org.omo.jms.RemotingFactory;

public class Server {

	private final DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
	
	private final int maxQueuedJobs = 100;
	private final int maxThreads = 100;
	private final int minThreads = 10;
	private final int numTrades = 100;
	private final int numPartitions = 3;
	private final int numDays = 5;
	private final int numAccounts = 5;
	private final int numCurrencies = 2;
	private final int numBalances= 100;
	private final int numCompanies = 10;
	private final int timeout = 60 * 1000; // 1 minute

	private static final Log LOG = LogFactory.getLog(Server.class);

	private final Executor executor =  new ThreadPoolExecutor(minThreads, maxThreads, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maxQueuedJobs));
	private final Adaptor<String, String> adaptor = new  Adaptor<String, String>() {@Override public String getKey(String value) {return value;}};

	private final Connection connection;
	private final Session session;

	private final MapModelView<String, String> metaModel;

	public Server(String serverName, ConnectionFactory connectionFactory) throws JMSException, SecurityException, NoSuchMethodException {
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// build MetaModel
		{
			String modelName = "MetaModel"; 
			Metadata<String,String> modelMetadata = new StringMetadata("Name");
			RemotingFactory<Model<String, String>> factory = new RemotingFactory<Model<String, String>>(session, Model.class, (Destination)null, timeout);
			metaModel = new MapModelView<String, String>(modelName, modelMetadata, adaptor);
			Queue queue = session.createQueue(serverName + "." + modelName);
			factory.createServer(metaModel, queue, executor);
			metaModel.update(Collections.singleton(metaModel.getName())); // The metaModel is a Model !
			LOG.info("Listening on: " + queue);
		}
		
		// we'' randomize trade dates out over the next week...
		// adding TradeFeed
		IntrospectiveMetadata<Integer, Trade> tradeMetadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		Model<Integer, Trade> tradeFeed;
		{
			String modelName = "TradeFeed";
			DateRange dateRange = new DateRange();
			Model<Integer, Trade> model = new Feed<Integer, Trade>(modelName, tradeMetadata, new IntegerRange(0, numTrades), 100L, new TradeFeedStrategy(dateRange, new IntegerRange(0, numAccounts), new IntegerRange(0, numCurrencies)));
			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
			Queue queue = session.createQueue(serverName+"."+modelName);
			serverFactory.createServer(model, queue, executor);
			model.start();
			metaModel.update(Collections.singleton(modelName));
			tradeFeed = model;
		}
		{
			RemotingFactory<Model<Integer, Trade>> serverFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
			List<View<Integer, Trade>> partitions = new ArrayList<View<Integer, Trade>>();
			Map<String, FilteredModelView<Integer, Trade>> aggregatedAccounts = new HashMap<String, FilteredModelView<Integer,Trade>>();
			for (int p=0; p<numPartitions; p++) {

				// make the Feeds
				{
					String feedName = "TradeFeed." + p;
					DateRange dateRange = new DateRange();
					Model<Integer, Trade> feed = new Feed<Integer, Trade>(feedName, tradeMetadata, new IntegerRange(0, numTrades), 100L, new TradeFeedStrategy(dateRange, new IntegerRange(0, numAccounts), new IntegerRange(0, numCurrencies)));
					RemotingFactory<Model<Integer, Trade>> serverFactory2 = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, (Destination)null, timeout);
					serverFactory2.createServer(feed, session.createQueue("Server."+feedName), executor);
					feed.start();
					metaModel.update(Collections.singleton(feedName));
					tradeFeed = feed;
				}

				String partitionName = "Trade."+p;
				//MapModelView<Integer, Trade> partition = new MapModelView<Integer, Trade>(partitionName, tradeMetadata, adaptor);
				Filter<Trade> filter = new Filter<Trade>() {

					@Override
					public boolean apply(Trade value) {
						return true;
					}
				};
				ModelView<Integer, Trade> partition = new FilteredModelView<Integer, Trade>(partitionName, tradeMetadata, filter);
				partitions.add(partition);
				serverFactory.createServer(partition, session.createQueue("Server."+partitionName), executor);
				partition.start();
				metaModel.update(Collections.singleton(partitionName));

				DateRange dateRange = new DateRange();
				
				for (Date d : dateRange.getValues()) {
					String dayName = partitionName+".Date."+dateFormat.format(d);
					Filter<Trade> valueDateFilter = new ValueDateFilter(d);
					ModelView<Integer, Trade> day = new FilteredModelView<Integer, Trade>(dayName, tradeMetadata, valueDateFilter);
					view(partition, day);
					serverFactory.createServer(day, session.createQueue("Server."+dayName), executor);
					day.start();
					metaModel.update(Collections.singleton(dayName));

					for (int a=0; a<numAccounts; a++) {
						String accountName = dayName+".Account."+a;
						Filter<Trade> f = new AccountFilter(a);
						ModelView<Integer, Trade> account= new FilteredModelView<Integer, Trade>(accountName, tradeMetadata, f);
						serverFactory.createServer(account, session.createQueue("Server."+accountName), executor);
						view(day, account);
						account.start();
						metaModel.update(Collections.singleton(accountName));

						String name2 = "Trade.Date."+dateFormat.format(d)+".Account."+a;
						FilteredModelView<Integer, Trade> model = aggregatedAccounts.get(name2);
						if (model == null) {
							model = new FilteredModelView<Integer, Trade>(name2, tradeMetadata, new IdentityFilter<Trade>());
							aggregatedAccounts.put(name2, model);
							serverFactory.createServer(model, session.createQueue("Server."+name2), executor);
							model.start();
							metaModel.update(Collections.singleton(name2));
							//model.register(new AmountAggregator(d, a));
						}
						view(account, model);
					}

					for (int c=0; c<numCurrencies; c++) {
						String currencyName = dayName+".Currency."+c;
						Filter<Trade> f = new CurrencyFilter(c);
						ModelView<Integer, Trade> currency= new FilteredModelView<Integer, Trade>(currencyName, tradeMetadata, f);
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
				public Collection<Account> createNewValues(Range<Integer> range) {
					Collection<Account> values = new ArrayList<Account>(range.size());
					for (int id : range.getValues())
						values.add(new Account(id, 0));
					return values;
				}

				@Override
				public Account createNewVersion(Account original) {
					return new Account(original.getId(), original.getVersion()+1);
				}

				@Override
				public Integer getKey(Account item) {
					return item.getId();
				}};
				Model<Integer, Account> feed = new Feed<Integer, Account>(feedName, metadata, new IntegerRange(0, numAccounts), 100L, strategy);
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
				public Collection<Currency> createNewValues(Range<Integer> range) {
					Collection<Currency> values = new ArrayList<Currency>(range.size());
					for (int id : range.getValues())
						values.add(new Currency(id, 0));
					return values;
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
				Model<Integer, Currency> feed = new Feed<Integer, Currency>(feedName, metadata, new IntegerRange(0, numCurrencies), 100L, strategy);
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
				public Collection<Balance> createNewValues(Range<Integer> range) {
					Collection<Balance> values = new ArrayList<Balance>(range.size());
					for (int id : range.getValues())
						values.add(new Balance(id, 0));
					return values;
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
				Model<Integer, Balance> feed = new Feed<Integer, Balance>(feedName, metadata, new IntegerRange(0, numBalances), 100L, strategy);
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
				public Collection<Company> createNewValues(Range<Integer> range) {
					Collection<Company> values = new ArrayList<Company>(range.size());
					for (int id : range.getValues())
						values.add(new Company(id, 0));
					return values;
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
				Model<Integer, Company> feed = new Feed<Integer, Company>(feedName, metadata, new IntegerRange(0, numCompanies), 100L, strategy);
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
