package org.omo.cash;

import java.math.BigDecimal;
import java.rmi.server.UID;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import org.apache.activemq.ActiveMQConnection;
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
import org.omo.core.Registration;
import org.omo.core.StringMetadata;
import org.omo.core.View;
import org.omo.core.MapModelView.Adaptor;
import org.omo.jms.RemotingFactory;

//  TODO:
// scalability:
// work out AMQ incantation to reduce threads - increase dates/accounts/currencies etc
// can we reduce footprint ? increase trade size and number of trades - how big can we go
// snapshotting and journalling - consider...
// how quickly can a snapshot be loaded ?
// how much impact does making in-vm listeners concurrent have on startup
// can we deserialise parts of the same snapshot concurrently ?
// investigate java6 optimisations
// what can we do to reduce filtering costs
// if we are only looking at a portion of all the trades (e.g. a 6 week window) does that reduce startup time ?

// functionality:
// run partitions from separate spring configs
// aggregate projections from all partitions
// produce and aggregate currency projections
// investigate mono/c# - java comms - xml/protocol-buffers/etc. ?

public class Server {

	private static final IdentityFilter<Trade> IDENTITY_FILTER = new IdentityFilter<Trade>();

	private final DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
	
	private final int maxQueuedJobs = 100;
	private final int maxThreads = 10;
	private final int minThreads = 10;
	private final int numTrades = 1000;
	private final int numPartitions = 2;
	private final int numDays = 5;
	private final int numAccounts = 2;
	private final int numCurrencies = 2;
	private final int numBalances= 100;
	private final int numCompanies = 10;
	private final int timeout = 60 * 1000; // 1 minute
	private final long feedPeriod = 100L; // millis

	private static final Log LOG = LogFactory.getLog(Server.class);

	private final Executor executor =  new ThreadPoolExecutor(minThreads, maxThreads, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maxQueuedJobs));
	private final Adaptor<String, String> adaptor = new  Adaptor<String, String>() {@Override public String getKey(String value) {return value;}};

	private final Connection connection;
	private final Session session;
	private final RemotingFactory<Model<Integer, Trade>> tradeRemotingFactory;

	private final MapModelView<String, String> metaModel;

	public Server(String serverName, ConnectionFactory connectionFactory) throws JMSException, SecurityException, NoSuchMethodException {
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		tradeRemotingFactory = new RemotingFactory<Model<Integer, Trade>>(session, Model.class, timeout);
		// build MetaModel
		{
			String name = serverName + ".MetaModel"; 
			Metadata<String,String> modelMetadata = new StringMetadata("Name");
			RemotingFactory<Model<String, String>> factory = new RemotingFactory<Model<String, String>>(session, Model.class, timeout);
			metaModel = new MapModelView<String, String>(name, modelMetadata, adaptor);
			remote(metaModel, factory);
		}
		
		// we'' randomize trade dates out over the next week...
		// adding TradeFeed
		IntrospectiveMetadata<Integer, Trade> tradeMetadata = new IntrospectiveMetadata<Integer, Trade>(Trade.class, "Id");
		String tradeFeedName = serverName + ".TradeFeed";
		Feed<Integer, Trade> tradeFeed;
		DateRange dateRange = new DateRange(numDays);
		{
			tradeFeed = new Feed<Integer, Trade>(tradeFeedName, tradeMetadata, new IntegerRange(0, numTrades), feedPeriod, new TradeFeedStrategy(dateRange, new IntegerRange(0, numAccounts), new IntegerRange(0, numCurrencies)));
			remote(tradeFeed, tradeRemotingFactory);
		}
		{
			List<View<Integer, Trade>> partitions = new ArrayList<View<Integer, Trade>>();
			for (int p=0; p<numPartitions; p++) {

				String partitionName = serverName + ".Trade."+p;
				ModelView<Integer, Trade> partition = new FilteredModelView<Integer, Trade>(partitionName, tradeMetadata, IDENTITY_FILTER);
				partitions.add(partition);
				remote(partition, tradeRemotingFactory);


				for (Date d : dateRange.getValues()) {
					String dayName = partitionName+".ValueDate="+dateFormat.format(d);
					Filter<Trade> valueDateFilter = new ValueDateFilter(d);
					ModelView<Integer, Trade> day = new FilteredModelView<Integer, Trade>(dayName, tradeMetadata, valueDateFilter);
					view(partitionName, day);
					remote(day, tradeRemotingFactory);

					for (int a=0; a<numAccounts; a++) {
						String accountName = dayName+".Account="+a;
						Filter<Trade> f = new AccountFilter(a);
						ModelView<Integer, Trade> account= new FilteredModelView<Integer, Trade>(accountName, tradeMetadata, f);
						view(dayName, account);
						remote(account, tradeRemotingFactory);
					}

//					for (int c=0; c<numCurrencies; c++) {
//						String currencyName = dayName+".Currency="+c;
//						Filter<Trade> currencyFilter = new CurrencyFilter(c);
//						ModelView<Integer, Trade> currency= new FilteredModelView<Integer, Trade>(currencyName, tradeMetadata, currencyFilter);
//						view(dayName, currency);
//						remote(currency, tradeRemotingFactory);
//					}
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
			view(tradeFeedName, partitioner);
		}

		// adding AccountFeed
//		{
//			String feedName = serverName + ".AccountFeed";
//			IntrospectiveMetadata<Integer, Account> metadata = new IntrospectiveMetadata<Integer, Account>(Account.class, "Id");
//			Feed.Strategy<Integer, Account> strategy = new Feed.Strategy<Integer, Account>(){
//				@Override
//				public Collection<Account> createNewValues(Range<Integer> range) {
//					Collection<Account> values = new ArrayList<Account>(range.size());
//					for (int id : range.getValues())
//						values.add(new Account(id, 0));
//					return values;
//				}
//
//				@Override
//				public Account createNewVersion(Account original) {
//					return new Account(original.getId(), original.getVersion()+1);
//				}
//
//				@Override
//				public Integer getKey(Account item) {
//					return item.getId();
//				}};
//				Model<Integer, Account> feed = new Feed<Integer, Account>(feedName, metadata, new IntegerRange(0, numAccounts), 100L, strategy);
//				RemotingFactory<Model<Integer, Account>> serverFactory = new RemotingFactory<Model<Integer, Account>>(session, Model.class, timeout);
//				remote(feed, serverFactory);
//		}
//		// adding CurrencyFeed
//		{
//			String feedName = serverName + ".CurrencyFeed";
//			Feed.Strategy<Integer, Currency> strategy = new Feed.Strategy<Integer, Currency>(){
//
//				@Override
//				public Collection<Currency> createNewValues(Range<Integer> range) {
//					Collection<Currency> values = new ArrayList<Currency>(range.size());
//					for (int id : range.getValues())
//						values.add(new Currency(id, 0));
//					return values;
//				}
//
//				@Override
//				public Currency createNewVersion(Currency original) {
//					return new Currency(original.getId(), original.getVersion()+1);
//				}
//
//				@Override
//				public Integer getKey(Currency item) {
//					return item.getId();
//				}};
//				Metadata<Integer, Currency> metadata = new IntrospectiveMetadata<Integer, Currency>(Currency.class, "Id");
//				Model<Integer, Currency> feed = new Feed<Integer, Currency>(feedName, metadata, new IntegerRange(0, numCurrencies), 100L, strategy);
//				RemotingFactory<Model<Integer, Currency>> serverFactory = new RemotingFactory<Model<Integer, Currency>>(session, Model.class, timeout);
//				remote(feed, serverFactory);
//		}
//		// adding BalanceFeed
//		{
//			String feedName = serverName + ".BalanceFeed";
//			Feed.Strategy<Integer, Balance> strategy = new Feed.Strategy<Integer, Balance>(){
//
//				@Override
//				public Collection<Balance> createNewValues(Range<Integer> range) {
//					Collection<Balance> values = new ArrayList<Balance>(range.size());
//					for (int id : range.getValues())
//						values.add(new Balance(id, 0));
//					return values;
//				}
//
//				@Override
//				public Balance createNewVersion(Balance original) {
//					return new Balance(original.getId(), original.getVersion()+1);
//				}
//
//				@Override
//				public Integer getKey(Balance item) {
//					return item.getId();
//				}};
//				Metadata<Integer, Balance> metadata = new IntrospectiveMetadata<Integer, Balance>(Balance.class, "Id");
//				Model<Integer, Balance> feed = new Feed<Integer, Balance>(feedName, metadata, new IntegerRange(0, numBalances), 100L, strategy);
//				RemotingFactory<Model<Integer, Balance>> serverFactory = new RemotingFactory<Model<Integer, Balance>>(session, Model.class, timeout);
//				remote(feed, serverFactory);
//		}
//		// adding CompanyFeed
//		{
//			String feedName = serverName + ".CompanyFeed";
//			Feed.Strategy<Integer, Company> strategy = new Feed.Strategy<Integer, Company>(){
//
//				@Override
//				public Collection<Company> createNewValues(Range<Integer> range) {
//					Collection<Company> values = new ArrayList<Company>(range.size());
//					for (int id : range.getValues())
//						values.add(new Company(id, 0));
//					return values;
//				}
//
//				@Override
//				public Company createNewVersion(Company original) {
//					return new Company(original.getId(), original.getVersion()+1);
//				}
//
//				@Override
//				public Integer getKey(Company item) {
//					return item.getId();
//				}};
//				Metadata<Integer, Company> metadata = new IntrospectiveMetadata<Integer, Company>(Company.class, "Id");
//				Model<Integer, Company> feed = new Feed<Integer, Company>(feedName, metadata, new IntegerRange(0, numCompanies), 100L, strategy);
//				RemotingFactory<Model<Integer, Company>> serverFactory = new RemotingFactory<Model<Integer, Company>>(session, Model.class, timeout);
//				remote(feed, serverFactory);
//		}
		
		// aggregate models
		
		// Total the trades for each day for a given account within a given partition
		for (int p=0; p<numPartitions; p++) {
			// build a projection for this account for the following days...
			String accountProjectionName = serverName + ".Trade." + p + ".AccountProjection";
			Metadata<Integer, Projection> accountProjectionMetadata = new ProjectionMetaData(dateRange);
			FilteredModelView<Integer, Projection> accountProjection= new FilteredModelView<Integer, Projection>(accountProjectionName, accountProjectionMetadata, new IdentityFilter<Projection>());
			remote(accountProjection, new RemotingFactory<Model<Integer, Projection>>(session, Model.class, timeout));
			
			for (int a=0; a<numAccounts; a++) {
				String accountTotalName = serverName + ".Trade." + p + ".Account="+a + ".Total";
				Metadata<Date, AccountTotal> accountTotalMetadata = new AccountTotalMetadata();
				FilteredModelView<Date, AccountTotal> accountTotal = new FilteredModelView<Date, AccountTotal>(accountTotalName, accountTotalMetadata, new IdentityFilter<AccountTotal>());
				remote(accountTotal, new RemotingFactory<Model<Date, AccountTotal>>(session, Model.class, timeout));
				
				// attach aggregators to Total models to power Projection models
				String projectionModelAggregator = "";
				ProjectionAggregator projectionAggregator = new ProjectionAggregator(projectionModelAggregator, dateRange, a, accountProjection);
				accountTotal.register(projectionAggregator);
				
				for (Date d : dateRange.getValues()) {
					String modelName = serverName + ".Trade." + p + ".ValueDate="+dateFormat.format(d)+".Account="+a;					
					String aggregatorName = serverName + ".Trade." + p + ".ValueDate="+dateFormat.format(d) + ".Account=" + a + ".Total";
					AmountAggregator aggregator = new AmountAggregator(aggregatorName, d, a, accountTotal);
					FilteredModelView<Integer, Trade> model = (FilteredModelView<Integer, Trade>)nameToModel.get(modelName);
					accountTotal.update(Collections.singleton(new AccountTotal(d, 0, a, new BigDecimal(0))));
					Registration<Integer, Trade> registration = model.register(aggregator);
					LOG.info("registering aggregator with: " + modelName + " and feeding: " + accountTotalName);
					for (Trade trade : registration.getData()) {
						aggregator.insert(trade);
					}
				}
			}
		}

		
		// Trades for a given Account for a given Day/Period (aggregated across all Partitions)
		for (Date d : dateRange.getValues()) {
			for (int a=0; a<numAccounts; a++) {
				String prefix = serverName + ".Trade";
				String suffix = ".ValueDate="+dateFormat.format(d)+".Account="+a;
				String accountsName = prefix + suffix;
				FilteredModelView<Integer, Trade> accounts = new FilteredModelView<Integer, Trade>(accountsName, tradeMetadata, IDENTITY_FILTER);
				remote(accounts, tradeRemotingFactory);
				for (int p=0; p<numPartitions; p++) {
					view(prefix + "." + p + suffix, accounts);
				}
			}
		}
//		// Trades for a given Currency for a given Day/Period (aggregated across all Partitions)
//		for (Date d : dateRange.getValues()) {
//			for (int c=0; c<numCurrencies; c++) {
//				String prefix = serverName + ".Trade";
//				String suffix = ".ValueDate="+dateFormat.format(d)+".Currency="+c;
//				String currenciesName = prefix + suffix;
//				FilteredModelView<Integer, Trade> currencies = new FilteredModelView<Integer, Trade>(currenciesName, tradeMetadata, IDENTITY_FILTER);
//				remote(currencies, tradeRemotingFactory);
//				for (int p=0; p<numPartitions; p++) {
//					view(prefix + "." + p + suffix, currencies);
//				}
//			}
//		}
	}

	protected final Map<String, Model<?, ?>> nameToModel = new HashMap<String, Model<?,?>>();
	
	protected void remote(Model model, RemotingFactory serverFactory) throws JMSException {
		String name = model.getName();
		nameToModel.put(name, model);
		Queue queue = session.createQueue(name);
		serverFactory.createServer(model, queue, executor);
		metaModel.update(Collections.singleton(name));
		model.start();
		LOG.info("Listening on: " + name);
	}

	protected void view(String modelName, View<Integer, Trade> view) throws IllegalArgumentException, JMSException {
		Model<Integer, Trade> model; 
		View<Integer, Trade> v;
		
		if (false) {
			model = tradeRemotingFactory.createSynchronousClient(modelName, true); 
			Destination clientDestination = session.createQueue("Client." + new UID().toString()); // tie up this UID with the one in RemotingFactory
			RemotingFactory<View<Integer, Trade>> serverFactory = new RemotingFactory<View<Integer, Trade>>(session, View.class, timeout);
			serverFactory.createServer(view, clientDestination, executor);
			v = serverFactory.createSynchronousClient(clientDestination, true);
		} else {
			model = (Model<Integer, Trade>)nameToModel.get(modelName);
			v = view;
		}

		Registration<Integer, Trade> registration = model.registerView(v);
		view.update(registration.getData());
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String name = (args.length == 0 ? "Server" : args[0]);
		String url = "peer://" + name + "/broker0?broker.persistent=false&useJmx=false";
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(url);
		System.setProperty("org.apache.activemq.UseDedicatedTaskRunner", "false");
		activeMQConnectionFactory.setOptimizedMessageDispatch(true);
		activeMQConnectionFactory.setObjectMessageSerializationDefered(true);
		activeMQConnectionFactory.setCopyMessageOnSend(false);
		ConnectionFactory connectionFactory = activeMQConnectionFactory;
		ActiveMQConnection c = (ActiveMQConnection)connectionFactory.createConnection();
		LOG.info("org.apache.activemq.UseDedicatedTaskRunner=" + System.getProperty("org.apache.activemq.UseDedicatedTaskRunner"));
		LOG.info("OptimizedMessageDispatch="+c.isOptimizedMessageDispatch());
		LOG.info("ObjectMessageSerializationDeferred="+c.isObjectMessageSerializationDefered());
		LOG.info("CopyMessageOnSend="+c.isCopyMessageOnSend());
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
