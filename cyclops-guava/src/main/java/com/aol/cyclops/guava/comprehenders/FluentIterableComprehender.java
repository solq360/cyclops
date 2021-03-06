package com.aol.cyclops.guava.comprehenders;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import com.aol.cyclops.internal.comprehensions.comprehenders.StreamableComprehender;
import com.aol.cyclops.types.extensability.Comprehender;
import com.aol.cyclops.util.stream.StreamUtils;
import com.google.common.collect.FluentIterable;

public class FluentIterableComprehender implements Comprehender<FluentIterable> {

	@Override
	public Object map(FluentIterable t, Function fn) {
		return t.transform(s -> fn.apply(s));
	}
	@Override
	public Object executeflatMap(FluentIterable t, Function fn){
		return flatMap(t,input -> unwrapOtherMonadTypes(this,fn.apply(input)));
	}
	@Override
	public Object flatMap(FluentIterable t, Function fn) {
		return t.transformAndConcat(s->fn.apply(s));
	}

	@Override
	public FluentIterable of(Object o) {
		return FluentIterable.of(new Object[]{o});
	}

	@Override
	public FluentIterable empty() {
		return FluentIterable.of(new Object[0]);
	}

	@Override
	public Class getTargetClass() {
		return FluentIterable.class;
	}
	
	@Override
	public Object resolveForCrossTypeFlatMap(Comprehender comp, FluentIterable apply) {
		if(comp instanceof com.aol.cyclops.internal.comprehensions.comprehenders.StreamComprehender || comp instanceof StreamableComprehender){
			return StreamUtils.stream(apply);
		}
		return Comprehender.super.resolveForCrossTypeFlatMap(comp, apply);
	}
	static FluentIterable unwrapOtherMonadTypes(Comprehender<FluentIterable> comp,Object apply){
		
		final Object finalApply = apply;
		if(apply instanceof java.util.stream.Stream)
			return FluentIterable.from( new Iterable(){ 
				public Iterator iterator(){
					return ((java.util.stream.Stream)finalApply).iterator();
				}
			});
		if(apply instanceof Iterable)
			return FluentIterable.from( ((Iterable)apply));
		
		if(apply instanceof Collection){
			return FluentIterable.from((Collection)apply);
		}
		
		return Comprehender.unwrapOtherMonadTypes(comp,apply);
		
	}
	@Override
	public FluentIterable fromIterator(Iterator o) {
		return FluentIterable.from(()->o);
	}

}
