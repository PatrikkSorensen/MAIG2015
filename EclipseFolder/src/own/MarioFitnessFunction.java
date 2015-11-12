package own;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Configuration;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TargetFitnessFunction;
import com.anji.integration.TranscriberException;
import com.anji.persistence.Persistence;
import com.anji.util.Arrays;
import com.anji.util.Configurable;
import com.anji.util.DummyConfiguration;
import com.anji.util.Properties;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.MarioAIOptions;

public class MarioFitnessFunction implements BulkFitnessFunction, Configurable {

	private static Logger logger = Logger.getLogger( TargetFitnessFunction.class );
	private ActivatorTranscriber factory;
	private int numTrials = 3;
	
	
	//MARIO VARIABLES
	static final MarioAIOptions marioAIOptions = new MarioAIOptions();
    static final BasicTask basicTask = new BasicTask(marioAIOptions);

    static Environment environment = MarioEnvironment.getInstance();
    static Agent agent = new NEATController();
    
    //Info on stage
    double[] state; 
    int zLevelScene = 0;
    protected byte[][] levelScene;
    //Control buttons
    boolean[] actions = new boolean[Environment.numberOfKeys]; 
    
	
	
	@Override
	public void init(Properties props) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("INITTING");
		factory = (ActivatorTranscriber) props.singletonObjectProperty( ActivatorTranscriber.class );		
		//marioAIOptions.printOptions(false);
	}

	@Override
	public void evaluate(List genotypes) {
		System.out.println("Evaluting list of chromosones..."); 
		Iterator it = genotypes.iterator();
		while ( it.hasNext() ) {
			Chromosome genotype = (Chromosome) it.next();
			evaluate(genotype, false);
		}
		
		
		//marioAIOptions
	}
	
	public void evaluate( Chromosome c, boolean visual ) {
		// Easy level: 
		//String options = "-lf off -zs 1 -ls 16 -vis on";
	    //environment.reset(options);
		
		// Reset environment each trial
		if(visual){
			marioAIOptions.setVisualization(true);
			environment.reset(marioAIOptions);
		} else {
			marioAIOptions.setVisualization(false);
			environment.reset(marioAIOptions);
		}
	    
	    try {
			Activator activator = factory.newActivator( c );

			// calculate fitness, sum of multiple trials
			int fitness = 0;
			for ( int i = 0; i < numTrials; i++ )
				fitness += singleTrial( activator );
			fitness /= numTrials;
			System.out.println("EVALUATE: fitness score,  " + fitness);
			c.setFitnessValue( fitness );
		}
		catch ( Throwable e ) {
			logger.warn( "error evaluating chromosome " + c.toString(), e );
			c.setFitnessValue( 0 );
		}
	}
	
	private double[] newState() {
		double[] state = new double[ 2 ];
		state[ 0 ] = state[ 1 ] =  0;
		return state;
	}
	
	private int singleTrial( Activator activator ) {
		state = newState();
		int fitness = 0;
		logger.debug( "state = " + Arrays.toString( state ) );
		double[] networkInput;
	    levelScene = environment.getLevelSceneObservationZ(zLevelScene);
		
		while(!environment.isLevelFinished()){
			//Set all actions to false
			resetActions();
			
//			#For each tick do
//				getState 
//				setInputs
				networkInput = new double[ state.length ];
				state = getStateFromStage();
				for(int i = 0; i < networkInput.length; i++) 
					networkInput[ i ] = state[ i ]; 
//				networkInput[ 0 ] = state[ 0 ]; 
//				networkInput[ 1 ] = state[ 1 ];
//				networkInput[ 2 ] = state[ 2 ]; 
//				networkInput[ 3 ] = state[ 3 ]; 
//				networkInput[ 4 ] = state[ 4 ]; 
//				networkInput[ 5 ] = state[ 5 ]; 
				
				//Give the network some inputs
				double[] networkOutput = activator.next(networkInput);
				//System.out.println("NetworkOutput[0]: " + networkOutput[0]);
				//System.out.println("NetworkOutput[1]: " + networkOutput[1]);
				//Perform some action based on networkOutput
				environment.performAction(getAction(networkOutput));
				makeTick();
				
	    }
		fitness = environment.getEvaluationInfo().distancePassedCells;
		//System.out.println("Fitness: " + fitness);
		//double networkOutput = activator.next( networkInput )[ 0 ];
		//logger.debug( "trial took " + currentTimestep + " steps" );
		return fitness;
	}
	
	public boolean[] getAction(double[] networkOutput){
		
		if(networkOutput[0] < 0.5)
			actions[Mario.KEY_LEFT] = false;
		else
			actions[Mario.KEY_LEFT] = true;
		
		if(networkOutput[1] < 0.5)
			actions[Mario.KEY_RIGHT] = false;
		else
			actions[Mario.KEY_RIGHT] = true;
		
		if(networkOutput[2] < 0.5)
			actions[Mario.KEY_DOWN] = false;
		else
			actions[Mario.KEY_DOWN] = true;
		
		if(networkOutput[3] < 0.5)
			actions[Mario.KEY_UP] = false;
		else
			actions[Mario.KEY_UP] = true;
		
		if(networkOutput[4] < 0.5)
			actions[Mario.KEY_JUMP] = false;
		else
			actions[Mario.KEY_JUMP] = true;
		
		if(networkOutput[5] < 0.5)
			actions[Mario.KEY_SPEED] = false;
		else
			actions[Mario.KEY_SPEED] = true;
		
		
		
//		for(int i = 0; i < networkOutput.length; i++){
//			if(networkOutput[i] < 0.5)
//				actions[i] = false;
//			else
//				actions[i] = true;
//		}
		
		return actions;
	}
	
	public void resetActions(){
	    actions[Mario.KEY_LEFT] = false;
	    actions[Mario.KEY_RIGHT] = false;
	    actions[Mario.KEY_DOWN] = false;
	    actions[Mario.KEY_UP] = false;
	    actions[Mario.KEY_JUMP] = false;
	    actions[Mario.KEY_SPEED] = false;
	}
	
	
	
	/*
	 * @return The state of the Mario World. Atm only the two by three blocks in front of Mario
	 */
	public double[] getStateFromStage(){
		
		double[] inputs =  { levelScene[9][10], levelScene[9][11]};
		
//		double[] inputs =  { 
//								levelScene[9][10], 
//								levelScene[9][11],
//								levelScene[9][12],
//								levelScene[8][10], 
//								levelScene[8][11],
//								levelScene[8][12]
//							};

		return inputs;
		
	}
	
	/*
	 * @return The state of the Mario World. Atm only the two by three blocks in front of Mario
	 */
	public double[][] getFullStateFromStage(){
		
		double[][] inputs = new double[18][18];
	    //GET INPUTS FROM STAGE
	    for(int i = 0; i<levelScene.length; i++){
			for(int j = 0; j<levelScene[i].length; j++){
				inputs[i][j] = levelScene[i][j];
			}
		}
		
		return inputs;
		
	}
	
	public void performAction(){
		
		environment.performAction(agent.getAction());	
		
	}
	
	public void makeTick(){
		environment.tick();
    	agent.integrateObservation(environment);
        
	}
	
	@Override
	public int getMaxFitnessValue() {
		// TODO Auto-generated method stub
		return 10;
	}
}
