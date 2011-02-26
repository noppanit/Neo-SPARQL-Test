import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.EmbeddedGraphDatabase;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Direction;


class neotest {

    public static void main(String args[]) {

	//NeoService neo = ... // Get factory
	GraphDatabaseService neo = new EmbeddedGraphDatabase( "var/graphdb" );  
	Transaction tx = neo.beginTransaction();

	    // Create Thomas 'Neo' Anderson
	Node mrAnderson = neo.createNode();
	mrAnderson.setProperty( "name", "Thomas Anderson" );
	mrAnderson.setProperty( "age", 29 );
	// Create Morpheus
	Node morpheus = neo.createNode();
	morpheus.setProperty( "name", "Morpheus" );
	morpheus.setProperty( "rank", "Captain" );
	morpheus.setProperty( "occupation", "Total bad ass" );
	// Create a relationship representing that they know each other
	mrAnderson.createRelationshipTo( morpheus, RelTypes.KNOWS );
	// ...create Trinity, Cypher, Agent Smith, Architect similarly
	
	tx.commit();

	// Instantiate a traverser that returns Mr Anderson's friends
	Traverser friendsTraverser = mrAnderson.traverse(
							 Traverser.Order.BREADTH_FIRST,
							 StopEvaluator.END_OF_NETWORK,
							 ReturnableEvaluator.ALL_BUT_START_NODE,
							 RelTypes.KNOWS,
							 Direction.OUTGOING );
	// Traverse the node space and print out the result
	System.out.println( "Mr Anderson's friends:" );
	for ( Node friend : friendsTraverser )
	    {
		System.out.printf( "At depth %d => %s%n",
				   friendsTraverser.currentPosition().getDepth(),
				   friend.getProperty( "name" ) );
	    }

	// Create a traverser that returns all “friends in love”
	Traverser loveTraverser = mrAnderson.traverse(
						      Traverser.Order.BREADTH_FIRST,
						      StopEvaluator.END_OF_NETWORK,
						      new ReturnableEvaluator()
						      {
							  public boolean isReturnableNode( TraversalPosition pos )
							  {
							      return pos.currentNode().hasRelationship(
									 RelTypes.LOVES, Direction.OUTGOING );
							  }
						      },
						      RelTypes.KNOWS,
						      Direction.OUTGOING );
	

	// Traverse the node space and print out the result
	System.out.println( "Who’s in love?" );
	for ( Node person : loveTraverser )
	    {
		System.out.printf( "At depth %d => %s%n",
				   loveTraverser.currentPosition().getDepth(),
				   person.getProperty( "name" ) );
	    }

	
    }

}