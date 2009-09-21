package org.omo.core;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractModel<OutputKey, OutputValue> implements Model<OutputKey, OutputValue> {

	protected final Log log = LogFactory.getLog(getClass());

	protected final String name;
	
	protected final Collection<View<OutputKey, OutputValue>> views = new ArrayList<View<OutputKey, OutputValue>>();
	
	protected abstract Collection<OutputValue> getData();
	
	public AbstractModel(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Collection<OutputValue> registerView(View<OutputKey, OutputValue> view) {
		log.debug("registering view: " + view);
		synchronized (views) {
			views.add(view);
		}
		return getData();
	}
	
	@Override
	public boolean deregisterView(View<OutputKey, OutputValue> view) {
		boolean success;
		synchronized (views) {
			success = views.remove(view);
		}
		if (success)
			log.debug("deregistered view: " + view);
		else
			log.warn("failed to deregister view: " + view);
		
		return success;
	}	
	
	protected void notifyUpsertion(OutputValue upsertion) {
		synchronized (views) {
			for (View<OutputKey, OutputValue> view : views)
				try {
					view.update(upsertion);
				} catch (RuntimeException e) {
					log.error("view notification failed: " + view + " <- " + upsertion, e);
				}
		}
	}

	protected void notifyUpsertion(Collection<OutputValue> upsertions) {
		for (View<OutputKey, OutputValue> view : views)
			try {
				view.batch(null, upsertions, null);
			} catch (RuntimeException e) {
				log.error("view notification failed: " + view + " <- " + upsertions, e);
			}
	}
}
