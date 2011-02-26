import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.rdf.sparql.MetaModelProxy;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.rdf.store.RdfStore;
import org.neo4j.rdf.store.VerboseQuadStore;
import org.neo4j.rdf.store.representation.standard.VerboseQuadStrategy;
import org.neo4j.rdf.store.representation.standard.VerboseQuadExecutor;
import org.neo4j.rdf.model.CompleteStatement;
import org.neo4j.rdf.model.Uri;
import org.neo4j.rdf.model.Literal;
import org.neo4j.rdf.model.Context;
import org.neo4j.rdf.model.WildcardStatement;
import org.neo4j.rdf.model.Wildcard;
import org.neo4j.rdf.model.Resource;
import org.neo4j.rdf.fulltext.FulltextIndex;
import org.neo4j.rdf.fulltext.SimpleFulltextIndex;
import name.levering.ryan.sparql.common.RdfBindingSet;
import name.levering.ryan.sparql.common.Variable;
import name.levering.ryan.sparql.parser.ParseException;
import name.levering.ryan.sparql.model.Query;
import name.levering.ryan.sparql.model.SelectQuery;
import org.neo4j.meta.model.*;
import org.neo4j.rdf.sparql.MetaModelProxyImpl;
import org.neo4j.rdf.sparql.Neo4jRdfBindingSet;
import org.neo4j.rdf.sparql.Neo4jBindingRow;
import org.neo4j.rdf.sparql.Neo4jVariable;
import org.neo4j.rdf.sparql.Neo4jSparqlEngine;
import org.neo4j.rdf.sparql.Neo4jRdfSource;

import java.io.StringReader;
import java.util.Iterator;
import java.io.File;


public class neotest {
    
    public static void main(String args[]) {
	//NeoService neo = ... // Get factory
	GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "var/graphdb" );  
	LuceneIndexService indexService = new LuceneIndexService( graphDb );
	FulltextIndex fulltextIndex = new SimpleFulltextIndex( graphDb,
							       new File( "var/graphdb/fulltext-index" ) );	
	String PERSON = "http://neo4j.org/person"; // resource
	String KNOWS = "http://neo4j.org/knows"; // predicate
	    String TYPE = "http://neo4j.org/type"; 
	String NAME = "http://neo4j.org/name"; // property 
	String NICK = "http://neo4j.org/nickname"; // property 

	MetaModel model = new MetaModelImpl( graphDb, indexService );
	RdfStore store = new VerboseQuadStore( graphDb, indexService, model, null );

	Transaction tx = graphDb.beginTx();
	try {		
		MetaModelNamespace namespace = model.getGlobalNamespace();
		// Create a class, use ", true" for "create it if it doesn't exist".
		MetaModelClass personClass = namespace.getMetaClass(PERSON, true );
		// Create a property in a similar way.
		MetaModelProperty nameProperty = namespace.getMetaProperty(NAME, true );
		MetaModelProperty typeProperty = namespace.getMetaProperty(TYPE, true );
		// Tell the meta model that persons can have name properties.
		personClass.getDirectProperties().add( nameProperty );
		personClass.getDirectProperties().add( typeProperty );
		MetaModelProperty nickName = namespace.getMetaProperty( NICK, true );
		nameProperty.getDirectSubs().add( nickName );
		
		ArrayList<CompleteStatement> statements = new ArrayList<CompleteStatement>();
		
		statements.add(new CompleteStatement((Resource) new Uri("http://neo4j.org/mattias"),
						     new Uri(TYPE), 
						     new Uri(PERSON),
						     Context.NULL));
		statements.add(new CompleteStatement((Resource) new Uri("http://neo4j.org/emil"),
						     new Uri(TYPE), 
						     new Uri(PERSON),
						     Context.NULL));
		statements.add(new CompleteStatement((Resource) new Uri("http://neo4j.org/mattias"),
						     new Uri(NAME), 
						     new Literal("Mattias"),
						     Context.NULL));
		statements.add(new CompleteStatement((Resource) new Uri("http://neo4j.org/emil"),
						     new Uri(NAME), 
						     new Literal("Emil"),
						     Context.NULL));
		statements.add(new CompleteStatement((Resource) new Uri("http://neo4j.org/emil"),
						     new Uri(NICK), 
						     new Literal("e"),
						     Context.NULL));
		
		store.addStatements(statements.toArray(new CompleteStatement[] {}));
		
		tx.success();
	    }
	finally
	    {
		tx.finish();
	    }

	Map<String, Integer> counts = new HashMap<String, Integer>();
	counts.put( PERSON, new Integer( 2 ) );
	Neo4jSparqlEngine engine = new Neo4jSparqlEngine(new VerboseQuadStrategy
							 (new VerboseQuadExecutor(graphDb, indexService, model, null), model), (MetaModelProxy) new MetaModelMockUp(model, counts));

	Query query = null;

	try {

	    query = engine.parse( new StringReader("SELECT ?p ?n " +
	    					   "WHERE { " +
	    					   "?p <http://neo4j.org/type> <http://neo4j.org/person> . " +
						   "?p <http://neo4j.org/nickname> ?n . }") );
	
	} catch(ParseException e) { System.out.println("Parse Exception: " + e);  }



	RdfBindingSet result =
	    ( ( SelectQuery ) query ).execute( new Neo4jRdfSource() );
	
	Iterator it = result.iterator();
	while(it.hasNext()) {
	    Neo4jBindingRow row = (Neo4jBindingRow)it.next();
	    for(Variable var: row.getVariables()) {
		System.out.println(var.getName() + ", " + row.getValue( var ).toString());
	    }
				       
	}


	System.out.println("done");

	store.shutDown();
	indexService.shutdown();
	graphDb.shutdown();
	
    }

}