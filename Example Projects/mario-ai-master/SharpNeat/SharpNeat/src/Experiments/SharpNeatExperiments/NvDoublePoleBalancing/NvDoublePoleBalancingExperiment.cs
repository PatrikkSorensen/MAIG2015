using System;
using System.Collections;

using SharpNeatLib.Evolution;
using SharpNeatLib.NeuralNetwork;

namespace SharpNeatLib.Experiments
{
	public class NvDoublePoleBalancingExperiment : IExperiment
	{
		IPopulationEvaluator populationEvaluator;
		IActivationFunction activationFunction = new SteepenedSigmoid();

		#region Constructor

		public NvDoublePoleBalancingExperiment()
		{
		}

		#endregion

		#region IExperiment Members
		
		/// <summary>
		/// This method is called immediately following instantiation of an experiment. It is used
		/// to pass in a hashtable of string key-value pairs from the 'experimentParameters' 
		/// block of the experiment configuration block within the application config file.
		/// 
		/// If no parameters where specified then an empty Hashtable is used.
		/// </summary>
		/// <param name="parameterTable"></param>
		public void LoadExperimentParameters(Hashtable parameterTable)
		{
		}

		public IPopulationEvaluator PopulationEvaluator
		{
			get
			{
				if(populationEvaluator==null)
					ResetEvaluator(activationFunction);

				return populationEvaluator;
			}
		}

		public void ResetEvaluator(IActivationFunction activationFn)
		{
			populationEvaluator = new SingleFilePopulationEvaluator(new NvDoublePoleBalancingNetworkEvaluator(), activationFn);
		}

		public int InputNeuronCount
		{
			get
			{
				return 3;
			}
		}

		public int OutputNeuronCount
		{
			get
			{
				return 1;
			}
		}

		public NeatParameters DefaultNeatParameters
		{
			get
			{
				NeatParameters np = new NeatParameters();

				np.populationSize = 1000;

				// Experimentally determined value. This gives some speciation from the outset.
				np.compatibilityThreshold = 1.6F; 

				np.targetSpeciesCountMin = 40;
				np.targetSpeciesCountMax = 50;

				// No hidden nodes are required to solve double pole balancing. Also the network is
				// only activated once per simulation timestep and so hidden nodes are essentially 
				// useless anyway - though not completely because network state is not reset per timestep.
				np.pMutateAddNode = 0.0;

				return np;
			}
		}

		public IActivationFunction SuggestedActivationFunction
		{
			get
			{
				return activationFunction;
			}
		}

		public AbstractExperimentView CreateExperimentView()
		{
			return null;
		}

		public string ExplanatoryText
		{
			get
			{
				return @"Double pole balancing with no velocity inputs. Evaluation is perofrmed from one initial state and therefore this experiment does not necessarily result in robust, generalised solutions capabale of handling different starting conditions.";
			}
		}

		#endregion
	}
}
