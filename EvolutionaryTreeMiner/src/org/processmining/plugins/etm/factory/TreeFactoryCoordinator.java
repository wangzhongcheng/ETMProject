// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.etm.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.model.narytree.Configuration;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.TreeUtils;
import org.uncommonseditedbyjoosbuijs.watchmaker.framework.factories.AbstractCandidateFactory;
/**
 * {@link org.uncommonseditedbyjoosbuijs.watchmaker.framework.CandidateFactory}
 * for generating trees of {@link NAryTree}s for the genetic programming example
 * application. Options are to randomly generate trees but also some predefined
 * structures are available.
 * 
 * @author jbuijs
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
public class TreeFactoryCoordinator extends AbstractCandidateFactory<NAryTree> {//implements EvolutionaryOperator<NAryTree> {
	protected CentralRegistry registry;
	private Map<TreeFactoryAbstract, Double> otherFactories;
	private double totalWeights;
	private double randomWeight;

	/**
	 * 
	 * @param registry
	 */
	public TreeFactoryCoordinator(CentralRegistry registry) {
		this(registry, null);
	}

	/**
	 * 
	 * @param registry
	 * @param randomWeight
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, double randomWeight) {
		this(registry, randomWeight, null);
	}

	/**
	 * 
	 * @param registry
	 * @param otherFactories
	 *            A map from other factory instances to the chance of them
	 *            creating a candidate. Please keep in mind that the chance of a
	 *            random candidate creation is now set as 1.0
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, Map<TreeFactoryAbstract, Double> otherFactories) {
		this(registry, 1, otherFactories);
	};

	/**
	 * 
	 * @param registry
	 * @param randomWeight
	 *            Chance of a random candidate
	 * @param otherFactories
	 *            A map from other factory instances to the chance of them
	 *            creating a candidate.
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, double randomWeight,
			Map<TreeFactoryAbstract, Double> otherFactories) {
		if (registry == null) {
			throw new IllegalArgumentException("The central bookkeeper can not be empty");
		}

		totalWeights = randomWeight;
		this.randomWeight = randomWeight;

		this.registry = registry;

		if (otherFactories == null) {
			this.otherFactories = new HashMap<TreeFactoryAbstract, Double>();
		} else {
			this.otherFactories = otherFactories;
		}

		for (Entry<TreeFactoryAbstract, Double> entry : this.otherFactories.entrySet()) {
			totalWeights += entry.getValue();
		}
	}

	/**
	 * Clears all other factories used (i.e. only this one remains)
	 */
	public void clearFactories() {
		otherFactories.clear();
		totalWeights = 0;
	}

	/**
	 * Allows additional factories to be added to the list of factories that can
	 * be triggered.
	 * 
	 * @param factory
	 * @param weight
	 */
	public void addFactory(TreeFactoryAbstract factory, double weight) {
		otherFactories.put(factory, weight);
		totalWeights += weight;
	}

	/**
	 * Update the random weight
	 * 
	 * @param randomWeight
	 */
	public void setRandomWeight(double randomWeight) {
		this.randomWeight = randomWeight;
	}

	/**
	 * Generates a random candidate by choosing a factory at random (including
	 * ourselves with weight 1). Uses random of CentralRegistry! not the
	 * provided!!!
	 */
	public NAryTree generateRandomCandidate(Random rng) {
		Double dice = registry.getRandom().nextDouble() * totalWeights;

		NAryTree tree = null;
//		/*********************************************/
//		ImportPtmlTest importPtmlTest=new ImportPtmlTest("D:\\processtree.ptml");
//		ProcessTree processTree=importPtmlTest.importPtmlFie();
//		ProcessTreeToNAryTree processTreeToNAryTree=new ProcessTreeToNAryTree();
//		tree=processTreeToNAryTree.convert(processTree);
//		
//		/*********************************************/
		if (dice < randomWeight) {
			//We're up!
			tree = generateRandomCandidate(registry);
		} else {
			//Try one of the other factories
			dice -= 1;
			for (Entry<TreeFactoryAbstract, Double> entry : otherFactories.entrySet()) {
				if (dice < entry.getValue()) {
					tree = entry.getKey().generateRandomCandidate(registry.getRandom());
					break;
				} else {
					dice -= entry.getValue();
				}
			}
		}

		if (tree == null) {
			assert false;
			tree = generateRandomCandidate(registry);
		}

		//Now make sure the tree has enough configurations, if required
		if (registry instanceof CentralRegistryConfigurable) {
			CentralRegistryConfigurable cr = (CentralRegistryConfigurable) registry;
			while (tree.getNumberOfConfigurations() < cr.getNrLogs()) {
				tree.addConfiguration(new Configuration(new boolean[tree.size()], new boolean[tree.size()]));
			}
		}
//		/************************************************************************/
//        System.out.println("######"+tree.toInternalString()+"####");
//        NAryTreeToProcessTree nAryTreeToProcessTree=new NAryTreeToProcessTree();
//        ProcessTree processTree2= nAryTreeToProcessTree.convert(tree);
//        System.out.println("&&&&"+processTree2.toString()+"&&&&");
//        
//        /**************************************************************************/
		  System.out.println("######"+tree.toInternalString()+"####");
		return tree;

	}

	/**
	 * Static function that randomly selects a method to generate a process
	 * model and returns the result
	 * 
	 * @param registry
	 * @return NAryTree created randomly or according to some pattern.
	 */
	public static NAryTree generateRandomCandidate(CentralRegistry registry) {
		return randomTree(registry);
	}

	/**
	 * Returns a random tree
	 * 
	 * @param registry
	 * @return
	 */
	public static NAryTree randomTree(CentralRegistry registry) {
		//TODO check probability and maximum size, correct guesses?
		//TEST with small trees of size 4 that then grow step by step by our mutators
		return TreeUtils.rewriteRevSeq(TreeUtils.randomTree(registry.nrEventClasses(), .4, 1, 4, registry.getRandom()));
	}
}
