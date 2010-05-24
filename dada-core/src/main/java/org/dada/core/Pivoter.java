package org.dada.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Pivoter<KO, VI, VO> extends AbstractModel<KO, VO> implements View<VI> {

	// TODO - we need to hold on to oldDelta so we can drop out of order updates etc...
	
	private final Creator<VO> creator;
	private final VO initialValue;
	private final AtomicReference<VO> datum;
	private final Getter<?, VO> verticalGetter;
	private final Strategy<VI, VO> strategy;
	
	public static interface Strategy<VI, VO> {
		VO pivot(VO oldValue, VI delta);
	}
	
	public Pivoter(String name, Metadata<KO, VO> metadata, Strategy<VI, VO> strategy, VO initialValue, Getter<?, VO> verticalGetter) {
		super(name, metadata);
		this.creator = metadata.getCreator(); // TODO: is a Creator the same as a Factory ?
		this.strategy = strategy;
		this.initialValue = initialValue;
		this.datum = new AtomicReference<VO>(this.initialValue);
		this.verticalGetter = verticalGetter;
	}

	@Override
	public Collection<VO> getData() {
		return Collections.singleton(datum.get());
	}

	@Override
	public void update(Collection<Update<VI>> insertions, Collection<Update<VI>> alterations, Collection<Update<VI>> deletions) {
		// somehow surmise the value and location to be poked...
		// read remaining values from old value
		// try to overwrite...
		
//		List<Getter<?, VO>> getters = metadata.getAttributeGetters();
//		Collection<Object> args = new ArrayList<Object>(getters.size());
//		VO oldValue;
//		VO newValue;
//		do {
//			oldValue = datum.get();
//			args.clear();
//			for (Getter<?, VO> getter : getters)
//				args.add(getter.get(oldValue));
//			// somehow poke in new value
//			// somehow increment version
//			newValue = creator.create(args);
//		} while (!datum.compareAndSet(oldValue, newValue));

		// need a logger
		// warn on empty update
		
		VI delta = null;

		for (Update<VI> insertion : insertions)
			delta = insertion.getNewValue(); 
		for (Update<VI> alteration : alterations)
			delta = alteration.getNewValue();
		for (Update<VI> deletion : deletions) {
			// TODO: what do we do on a deletion?
		}
		
		
		VO oldValue;
		VO newValue;
		do {
			oldValue = datum.get();
			// N.B. strategy-fn may be executed more than once, so MUST be a PURE FUNCTION
			newValue = strategy.pivot(oldValue, delta);
		} while (!datum.compareAndSet(oldValue, newValue));
		
	}

}
