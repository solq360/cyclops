package com.aol.cyclops.javaslang;

import java.util.stream.Stream;


import com.aol.cyclops.control.Xor;
import com.aol.cyclops.types.MonadicValue;
import com.aol.cyclops.types.MonadicValue2;

import javaslang.concurrent.Future;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;
import javaslang.control.Validation;



public class FromCyclopsReact {
	public static <T> javaslang.collection.Stream<T> fromStream(
			Stream<T> s) {
		return javaslang.collection.Stream.ofAll(()->s.iterator());
	}
	public static <T> Try<T> toTry(MonadicValue<T> value){
		return value.toTry().visit(s->Try.success(s),f->Try.failure(f));
		
	}
	public static <T> Future<T> future(MonadicValue<T> value){
		return Future.of(()->value.get());
	}
	public static <T> Option<T> option(MonadicValue<T> value){
		return Option.ofOptional(value.toOptional());
	}
	public static <L,R> Either<L,R> either(MonadicValue2<L,R> value){
		Xor<L,R> xor = (Xor)value.toXor();
		return xor.visit(l->Either.left(l),r->Either.right(r));
	}
	
	public static <L,R> Validation<L,R> validation(MonadicValue2<L,R> value){
		return Validation.fromEither(either(value));
	}


}
