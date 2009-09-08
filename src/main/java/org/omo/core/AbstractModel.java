package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractModel<Output> implements Model<Output> {

	protected final Log log = LogFactory.getLog(getClass());

	protected final Collection<View<Output>> views = new ArrayList<View<Output>>();
	
	protected abstract Collection<Output> getData();
	
	@Override
	public Collection<Output> registerView(View<Output> view) {
		views.add(view);
		return new ArrayList<Output>(getData()); // HashMap.values() is not serializable !
	}
	
	@Override
	public void deregisterView(View<Output> view) {
		views.remove(view);
	}
	
	protected void notifyUpsertion(Output upsertion) {
		for (View<Output> view : views)
			try {
				view.upsert(upsertion);
			} catch (RuntimeException e) {
				log.error("view notification failed: " + view + " <- " + upsertion, e);
			}
	}

	protected void notifyUpsertion(Collection<Output> upsertions) {
		for (View<Output> view : views)
			try {
				view.upsert(upsertions);
			} catch (RuntimeException e) {
				log.error("view notification failed: " + view + " <- " + upsertions, e);
			}
	}
}
