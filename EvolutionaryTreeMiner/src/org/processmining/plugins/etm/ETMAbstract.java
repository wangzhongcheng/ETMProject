package org.processmining.plugins.etm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener;
import org.processmining.framework.providedobjects.ProvidedObjectDeletedException;
import org.processmining.framework.providedobjects.ProvidedObjectID;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.live.ETMLiveListener.ListernerList;
import org.processmining.plugins.etm.live.ETMLiveListener.RunningState;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommonseditedbyjoosbuijs.watchmaker.framework.TerminationCondition;

/**
 * Abstract implementation of the ETM algorithm which can be implemented by
 * certain more specific ETM algorithms.
 * 
 * @author jbuijs
 * 
 * @param <R>
 *            Result type of the ETM algorithm implementation (e.g. {@link Tree}
 *            or {@link ParetoFront}).
 */
public abstract class ETMAbstract<R> implements ProvidedObjectLifeCycleListener, Runnable,
		Serializable {

	@SuppressWarnings("unused") //It is!
	private ETMParamAbstract<R> params;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Indicates the current state of the algorithm.
	 */
	protected transient RunningState currentState = RunningState.NOT_STARTED;

	/**
	 * Termination conditions that caused the alg. to stop
	 */
	protected transient List<TerminationCondition> satisfiedTerminationConditions;

	/**
	 * The result
	 */
	protected transient R result;

	/**
	 * Our ProM object ID to check if we are deleted when we get a deletion
	 * message
	 */
	protected transient ProvidedObjectID ourProMObjectID;

	/**
	 * The provided object manager to which we will ask our ProMObjectID once we
	 * are so far
	 */
	protected transient ProvidedObjectManager manager = null;

	/**
	 * Re-evaluate the list of trees using the provided evaluator to make sure
	 * that we use the same dimensions, metrics, weights, etc. etc. Should be
	 * called before creating the engine in the run() method.
	 * 使用提供的评估器重新评估树的列表，以确保我们使用相同的维度、指标、权重等等，在运行run()方法中创建引擎之前应该调用这些树。
	 * @param seed
	 *            List of trees to re-evaluate
	 * @param eval
	 *            Evaluator to use
	 */
	public static void reEvaluateSeed(List<NAryTree> seed, TreeFitnessAbstract evaluator) {
		for (NAryTree t : seed) {
			evaluator.getFitness(t, null);
		}
	}

	/**
	 * Returns the final result (or NULL if there is no result yet).
	 * 
	 * @return R result
	 */
	public R getResult() {
		return result;
	}

	/**
	 * Returns the termination conditions that caused the genetic algorithm to
	 * stop. Can be inspected when the ETM finished running to see why it
	 * stopped.
	 * 
	 * @return the satisfiedTerminationConditions
	 */
	public List<TerminationCondition> getSatisfiedTerminationConditions() {
		return satisfiedTerminationConditions;
	}

	/**
	 * Returns a string of the satisfied termination conditions
	 * 
	 * @return
	 */
	public String getTerminationDescription() {
		String output = "";

		List<TerminationCondition> list = getSatisfiedTerminationConditions();
		for (TerminationCondition satisfiedCondition : list) {
			output += satisfiedCondition.toString() + ", ";
		}

		return output;
	}

	/**
	 * 
	 * 
	 * @return the isRunning TRUE if the ETM is still running
	 */
	public boolean isRunning() {
		return currentState.equals(RunningState.RUNNING);
	}

	public RunningState getCurrentState() {
		return currentState;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener#providedObjectCreated(org.processmining.framework.providedobjects.ProvidedObjectID,
	 *      org.processmining.framework.plugin.PluginContext)
	 */
	public void providedObjectCreated(ProvidedObjectID objectID, PluginContext context) {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener
	 * #providedObjectFutureReady(org.processmining.framework.providedobjects.
	 * ProvidedObjectID)
	 */
	public void providedObjectFutureReady(ProvidedObjectID objectID) {
		if (manager != null) {
			try {
				Object o = manager.getProvidedObjectObject(objectID, true);

				if (o == this) {
					//Remember this ID
					ourProMObjectID = objectID;
				}
			} catch (ProvidedObjectDeletedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener
	 * #providedObjectDeleted(org.processmining.framework.providedobjects.
	 * ProvidedObjectID)
	 */
	public void providedObjectDeleted(ProvidedObjectID objectID) {
		if (objectID == ourProMObjectID) {
			//STOP SELF
			currentState = RunningState.USERCANCELLED;
			getParams().getListeners().fireFinished(currentState);
		}
	}

	public abstract ETMParamAbstract<R> getParams();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener
	 * #providedObjectNameChanged(org.processmining.framework.providedobjects.
	 * ProvidedObjectID)
	 */
	public void providedObjectNameChanged(ProvidedObjectID objectID) {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener
	 * #providedObjectObjectChanged(org.processmining.framework.providedobjects.
	 * ProvidedObjectID)
	 */
	public void providedObjectObjectChanged(ProvidedObjectID objectID) {
		// TODO Auto-generated method stub
	}

	/*-
	 * PREVENT SERIALIZATION OF THIS OBJECT
	 */

	@SuppressWarnings("unused")
	private void writeObject(ObjectOutputStream out) throws IOException {
		//throw new NotSerializableException();
	}

	@SuppressWarnings("unused")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		//throw new NotSerializableException();
	}

	@SuppressWarnings({ "unused" })
	private void readObjectNoData() throws ObjectStreamException {
		//throw new NotSerializableException();
	}

	public abstract ListernerList<R> getListenerList();

}
