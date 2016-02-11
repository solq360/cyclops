package com.aol.cyclops.matcher;

import static com.aol.cyclops.matcher2.Predicates.__;
import static com.aol.cyclops.matcher2.Predicates.type;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.aol.cyclops.control.Eval;
import com.aol.cyclops.control.Maybe;
import com.aol.cyclops.matcher2.Matchable;
import com.aol.cyclops.matcher2.Predicates;
import com.aol.cyclops.matcher2.Matchable.MatchSelf;
import com.aol.cyclops.matcher2.Matchable.MatchableTuple2;
import com.aol.cyclops.matcher2.Matchable.MatchableTuple3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;


public class MatchableTest {

	private String concat(String a,String b){
		return a+","+b;
	}
	private boolean isValidStreet(String street1){
		return true;
	}
	private boolean isValidHouse(int house){
		return true;
	}
	@Test
	public void matchTestStructuralAndGuards(){
	
		String v  =new Address(10,"hello","my city").match()
							   			 .on$12_()
							   			 .when((house,street)-> 
							   			 	house.<String>mayMatch(c->c.justWhere(in->"valid house",this::isValidHouse))
							   		            	 .recover("incorrectly configured house")
							   		            	 .ap2(this::concat)
							   		            	 .ap(
							   		      				street
							   		      						.<String>mayMatch(c->c.where(in->"valid street",this::isValidStreet))
							   		      						
							   		      				.recover("incorrectly configured steet")
							   		            	 )
							   		).get();
		
		assertThat(v,equalTo("valid house,valid street"));
		
							   		
	}
	@Test
	public void matchTestStructuralOnly(){
		String v =new Address(10,"hello","my city")
										 .match()
							   			 .on$12_()
							   			 .when((house,street)-> 
							   					house.filter(this::isValidHouse)
							   						 .map(i->"valid house")
							   						 .recover("incorrectly configured house")
							   		                 .ap2(this::concat)
							   		            	 .ap(street.filter(this::isValidStreet)
							   		            			   .map(s->"valid street")
							   		            			   .recover("incorrectly configured steet"))
							   		            	 .get());
		
		
		assertThat(v,equalTo("valid house,valid street"));
							   		
	}
	
	@Test
	public void matchTestNestedStructural(){
		String v = new Customer("test",new Address(10,"hello","my city"))
										 .match()
							   			 .on$_2()
							   			 .when(address ->
							   			   		 address.on$12_()
							   					   		.when((house,street)-> 
							   									house.filter(this::isValidHouse)
							   										 .map(i->"valid house")
							   										 .recover("incorrectly configured house")
							   									 	 .ap2(this::concat)
							   									 	 .ap(street.filter(this::isValidStreet)
							   									 			   .map(s->"valid street")
							   									 			   .recover("incorrectly configured steet"))
							   									 	 .get())
							   			 , ()->"no address configured");
		
		assertThat(v,equalTo("valid house, valid street"));
							   	
	}
	@AllArgsConstructor
	static class Address{
		int house;
		String street;
		String city;
		
		public MatchableTuple3<Integer,String,String> match(){
			return Matchable.from(()->house,()->street,()->city);
		}
	}
	@AllArgsConstructor
	static class Customer{
		String name;
		Address address;
		public MatchableTuple2<String,MatchableTuple3<Integer,String,String>> match(){
			return Matchable.from(()->name,()->Maybe.ofNullable(address).map(a->a.match()).orElseGet(()->null));
		}
	}
	@Test
	public void testMatch(){
		
		Matchable.of(new NestedCase(1,2,new NestedCase(3,4,null)))
		 			.matches(c->c.values(in->"2",1,__,Predicates.values(3,4,__)))
		 			.get();
		Matchable.of(new NestedCase(1,2,new NestedCase(3,4,null)))
			.matches(c->c.values(in->"2",1,__,type(NestedCase.class).values(3,4,__)))
			.get();
		Matchable.of(Arrays.asList(1,2,3))
					.matches(c->c.values(in->"2",1,__,3));
		Matchable.of(Arrays.asList(1,2,3))
					
					.matches(c->c.where(in->"2",t->t.equals(1),Predicates.__,t->t.equals(3)));
		
		Matchable.of(Arrays.asList(1,2,3))
					.matches(c->c.match(in->"2",equalTo(1),any(Integer.class),equalTo(4)));
		
		
		
	}
	@Value
	static class MyCase<R>  implements MatchSelf<MyCase<R>>{
		int a;
		int b;
		int c;
	}
	@Value
	static class NestedCase <R> implements MatchSelf<MyCase<R>>{
		int a;
		int b;
		NestedCase<R> c;
	}
	@Test
	public void singleCase(){
		
		
		Eval<Integer> result = Matchable.of(Optional.of(1))
										.matches(c->c.values(in->2,1));
		
		assertThat(result,equalTo(Eval.now(2)));
	}
	@Test(expected=NoSuchElementException.class)
	public void singleCaseFail(){
		 Matchable.of(Optional.of(2))
				   .matches(c->c.values(in->2,1));
		
		fail("exception expected");
	}
	@Test
	public void cases2(){
		Eval<Integer> result = Matchable.listOfValues(1,2)
								
								.matches(c->c.values(in->2,1,3)
											 .values(in->3,1,2));
		
		assertThat(result,equalTo(Eval.now(3)));
	}
	@Test 
	public void matchable(){
		Maybe<Integer> result = Matchable.of(Optional.of(1))
											.mayMatch(c->c.values(in->2,2));
	
		assertThat(result
						 .matches(c->c.isEmpty( in -> "hello")).get(),equalTo("hello"));
		
	}
	@Test 
	public void optionalMatch(){
		Eval<Integer> result2 = Matchable.of(Optional.of(1))
									
									.matches(c->c.values(in->2,1));
		
		assertThat(result2,equalTo(2));
	}
	@Test 
	public void emptyList(){
		
		assertThat(Matchable.of(Arrays.asList()).matches(c->c.isEmpty(in->"hello")),equalTo(Eval.now("hello")));
	}
	@Test 
	public void emptyStream(){
		
		assertThat(Matchable.of(Stream.of()).matches(c->c.isEmpty(in->"hello")),equalTo(Eval.now("hello")));
	}
	@Test 
	public void emptyOptional(){
		
		assertThat(Matchable.of(Optional.empty()).matches(c->c.isEmpty(in->"hello")),equalTo(Eval.now("hello")));
	}
	@Test
	public void emptyOptionalMultiple2(){
		assertThat(Matchable.of(Optional.empty())
				            .matches(
				            			o-> o.isEmpty(in->"hello")
				            			     .values(in->"2",1)
				            		)
				            		,equalTo("hello"));
		
		
	}
	@Test
	public void emptyOptionalMultiple3(){
		assertThat(Matchable.of(Optional.empty())
							
				            .matches(
				            			o-> o.isEmpty(in->"hello")
				            			     .values(i->""+2,1)
				            			     .values(i->""+3,2)
				            		).get()
				            		,equalTo("hello"));
		
		
	}
	@Test
	public void emptyMaybeMultiple3(){
		assertThat(Maybe.none()
						.matches(
				            			o-> o.isEmpty(in->"hello")
				            			      .values(in->"2",1)
				            			      .values(in->"3",2)
				            		)
				            		,equalTo("hello"));
		
		
	}
	@Test
	public void emptyOptionalMultiple4(){
		assertThat(Matchable.of(Optional.of(3))
							
				            .matches(
				            			o-> o.isEmpty(i->"hello")
				            			     .values(i->""+2,1)
				            			     .values(i->""+3,2)
				            			     .values(i->""+4,3)
				            		)
				            		,equalTo("4"));
		
		
	}
	@Test
	public void emptyOptionalMultiple5(){
		assertThat(Matchable.of(Optional.of(4))
								
								.matches(
				            			o-> o.isEmpty(i->"hello")
				            			.values(i->""+2,1)
				            			.values(i->""+3,2)
				            			.values(i->""+4,3)
				            			.values(i->""+5,4)
				            		)
				            		,equalTo("5"));
		
		
	}
	@Test 
	public void emptyOptionalMaybe(){
		
		assertThat(Matchable.of(Optional.empty()).mayMatch(c->c.isEmpty(i->"hello")).get(),equalTo("hello"));
	}
	@Test
	public void emptyOptionalMultiple2Maybe(){
		assertThat(Matchable.of(Optional.empty())
				            .mayMatch(
				            			o-> o.isEmpty(i->"hello")
				            			     .values(i->""+2)
				            		).get()
				            		,equalTo("hello"));
		
		
	}
	@Test
	public void emptyOptionalMultiple3Maybe(){
		assertThat(Matchable.of(Optional.empty())
				            .mayMatch(
				            			o-> o.isEmpty(i->"hello")
				            			     .values(i->""+2)
				            			     .values(i->""+3)
				            		).get()
				            		,equalTo("hello"));
		
		
	}
	@Test
	public void emptyOptionalMultiple4Maybe(){
		assertThat(Matchable.of(Optional.of(3))
				            .mayMatch(
				            			o-> o.isEmpty(i->"hello")
				            			     .values(i->""+2,1)
				            			     .values(i->""+3,2)
				            			     .values(i->""+4,3)
				            		).get()
				            		,equalTo("4"));
		
		
	}
	@Test
	public void emptyOptionalMultiple5Maybe(){
		assertThat(Matchable.of(Optional.of(4))
				            .mayMatch(
				            			o-> o.isEmpty(i->"hello")
				            			     .values(i->""+2,1)
				            			     .values(i->""+3,2)
				            			     .values(i->""+4,3)
				            			     .values(i->""+5,4)
				            		).get()
				            		,equalTo("5"));
		
		
	}
	public void matchByType(){
		
		assertThat(Matchable.of(1)
				                    .matches(c->c.justMatch(in->"hello",instanceOf(Integer.class))),
				                    equalTo("hello"));
	}
	public void matchListOfValues(){
		assertThat(Matchable.listOfValues(1,2,3)
							        .matches(c->c.where(i->2,(Object i)->(i instanceof Integer))),
							        equalTo(2));
		
	}
	@Test
	public void recursive(){
		Eval<String> result = Matchable.listOfValues(1,new MyCase(4,5,6))
				 				.matches(c->c.values(i->"rec",Predicates.__,Predicates.values(4,5,6)));
		
		assertThat(result.get(),equalTo("rec"));
	}
	
	@Test
	public void matchType(){
		
		Eval<Integer> result = Matchable.of(new Child(10,20)).matches(
									c-> c.justWhere(in->10, Predicates.type(Child.class).values(10,20)));
		
		assertThat(result,equalTo(10));
	}
	
	@Value
	static class Child extends Parent{
		int nextVal;
		public Child(int val,int nextVal) { super(val); this.nextVal = nextVal;}
	}
	@AllArgsConstructor(access=AccessLevel.PACKAGE)
	static class Parent{
		int val;
	}
	
}
