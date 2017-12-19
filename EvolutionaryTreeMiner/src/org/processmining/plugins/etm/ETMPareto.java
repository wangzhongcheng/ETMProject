package org.processmining.plugins.etm;

import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.plugins.etm.engines.ParetoEngine;
import org.processmining.plugins.etm.live.ETMLiveListener.ListernerList;
import org.processmining.plugins.etm.live.ETMLiveListener.RunningState;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.plugins.etm.parameters.ETMParamPareto;

/**
 * Implementation of the ETM algorithm that results in a Pareto Front of
 * {@link Tree}s. For an explanation of the Pareto front see <a
 * href=http://en.wikipedia
 * .org/wiki/Pareto_front#Pareto_efficiency_in_short>Wikipedia on Pareto
 * Front</a> for instance.
 * 
 * 执行ETM算法，结果在{@ link树}的帕累托前。对于帕累托阵线的解释，可以在帕累托的前面看到维基百科。
 * @author jbuijs
 * 
 */
//FIXME extend
public class ETMPareto extends ETMAbstract<ParetoFront> {

	private static final long serialVersionUID = 1L;

	//Our precious params
	protected transient final ETMParamPareto params;

	/*-
	private Canceller canceller = new Canceller() {
		public boolean isCancelled() {
			return false;
		}
	};/**/

	/**
	 * Instantiate the ETM Pareto algorithm with the provided parameters.
	 * 用所提供的参数实例化ETM Pareto算法。
	 * @param parameters
	 *            The ETM Pareto parameters object to initialize the ETM
	 *            algorithm with. These parameters can not be changed once the
	 *            algorithm started.
	 *            ETM Pareto参数对象初始化ETM算法。一旦算法开始，这些参数就不会改变。
	 */
	public ETMPareto(ETMParamPareto parameters) {
		this.params = parameters;
	}

	/**
	 * Instantiate the ETM Pareto algorithm with the provided parameters.
	 * Additionally provide a ProM {@link ProvidedObjectManager} in order to run
	 * in LIVE mode, e.g. updating the view of the Pareto front at each update
	 * of the ETM.
	 * 用所提供的参数实例化ETM Pareto算法。
	 * 此外，还提供一个ProM {@link ProvidedObjectManager}，以便在
	 * 实时模式中运行，例如在ETM的每个更新中更新Pareto front的视图。
	 * @param parameters
	 *            The ETM Pareto parameters object to initialize the ETM
	 *            algorithm with. These parameters can not be changed once the
	 *            algorithm started.
	 *            ETM Pareto参数对象初始化ETM算法。一旦算法开始，这些参数就不会改变。
	 * @param manager
	 *            The Provided Object Manager from a ProM Context. Required if
	 *            you want to run the ETM in 'live' mode.
	 *            所提供的对象管理器来自一个ProM上下文。如果你想在“实时”模式下运行ETM。
	 */
	public ETMPareto(ETMParamPareto parameters, ProvidedObjectManager manager) {
		this.params = parameters;
		this.manager = manager;
	}

	/**
	 * Run the ETM algorithm with the provided parameters
	 * 使用提供的参数运行ETM算法
	 */
	@Override
	public void run() {
		/*
		 * We need to recalculate the seed to get an overall fitness that
		 * corresponds to our weights. The ED will be 0 since each seed is in
		 * the list of trees to be compared and therefore its ED will be 0 (but
		 * it will influence the overall fitness)
		 * 我们需要重新计算种子，以获得与我们的体重相对应的整体适应性。
		 * ED将是0，因为每个种子都在树的列表中被比较，因此它的ED将是0(但它会影响整体的适应度)
		 */

		reEvaluateSeed(params.getSeed(), params.getFitnessEvaluator());
		System.out.println("*********"+params.getSeed().size()+"*********");

		/*
		 * Instantiate a new Watchmaker evolution engine.
		 */
		/*-
		ParetoEngine engine = new ParetoEngine(params.getCentralRegistry(), params.getFactory(),
				new EvolutionPipeline<NAryTree>(params.getEvolutionaryOperators()), params.getFitnessEvaluator(),
				params.getParetoFitnessEvaluator(), params.getSelectionStrategy(), params.getRng());/**/
		ParetoEngine engine = new ParetoEngine(params);

		//Set the preliminary pareto front result for live views
		this.result = engine.getParetoFront();

		//Start the engine!
		currentState = RunningState.RUNNING;
		engine.evolve();
		result = engine.getParetoFront();

		if (!currentState.equals(RunningState.USERCANCELLED)) {
			/*
			 * Only switch to state 'terminated' if we did not get here after a
			 * user cancellation (which is a termination condition in itself...)
			 */
			currentState = RunningState.TERMINATED;
		}
		this.satisfiedTerminationConditions = engine.getSatisfiedTerminationConditions();
	}

	public ListernerList<ParetoFront> getListenerList() {
		return params.getListeners();
	}

	public ETMParamAbstract<ParetoFront> getParams() {
		return params;
	}
}
