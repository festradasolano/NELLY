/*
 * Copyright 2018 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.edu.unicauca.dtm.nelly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.Classifier;
import moa.classifiers.functions.AdaGrad;
import moa.classifiers.functions.SGD;
import moa.classifiers.meta.RandomRules;
import moa.classifiers.rules.AMRulesRegressor;
import moa.classifiers.rules.functions.AdaptiveNodePredictor;
import moa.classifiers.rules.functions.FadingTargetMean;
import moa.classifiers.rules.functions.LowPassFilteredLearner;
import moa.classifiers.rules.functions.Perceptron;
import moa.classifiers.rules.functions.TargetMean;
import moa.classifiers.rules.meta.RandomAMRules;
import moa.classifiers.trees.FIMTDD;
import moa.classifiers.trees.ORTO;
import moa.streams.ArffFileStream;

/**
 * 
 * 
 * Copyright 2018 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0 (see LICENSE for details)
 * 
 * @author festradasolano
 */
public class MOARegressor {

	/**
	 * 
	 */
	private static final Map<String, Integer> options;
	static {
		options = new HashMap<String, Integer>();
		options.put("--help", 0);
		options.put("--arff", 1);
		options.put("--out", 2);
		options.put("--learner", 3);
		options.put("--idxClass", 4);
		options.put("--idxTrain", 5);
		options.put("--thrTrain", 6);
	}

	private static final Map<String, Integer> learnerOptions;
	static {
		learnerOptions = new HashMap<String, Integer>();
		learnerOptions.put("adagrad", 0);
		learnerOptions.put("sgd", 1);
		learnerOptions.put("randomrules", 2);
		learnerOptions.put("amrules", 3);
		learnerOptions.put("adaptivenode", 4);
		learnerOptions.put("fadingtarget", 5);
		learnerOptions.put("lowpassfiler", 6);
		learnerOptions.put("perceptron", 7);
		learnerOptions.put("targetmean", 8);
		learnerOptions.put("randomamrules", 9);
		learnerOptions.put("fimtdd", 10);
		learnerOptions.put("orto", 11);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Define default paths
		String arffPath = System.getProperty("user.home") + File.separator + "data.arff";
		String outPath = System.getProperty("user.home") + File.separator + "out.csv";
		//
		String sLearner = "fimtdd";
		//
		String sIndexClass = "1";
		String sIndexTrain = "-1";
		String sThresholdTrain = "0";
		// Get parameters from arguments
		for (int i = 0; i < args.length; i++) {
			// Check that given option exists
			int option = 0;
			if (options.containsKey(args[i])) {
				option = options.get(args[i]);
				i++;
			} else {
				System.out.println("Option " + args[i] + " does not exist");
				printHelp();
				System.exit(1);
			}
			// Set parameter corresponding to option
			switch (option) {
			case 0:
				printHelp();
				System.exit(1);
				break;
			case 1:
				arffPath = args[i];
				break;
			case 2:
				outPath = args[i];
				break;
			case 3:
				sLearner = args[i];
				break;
			case 4:
				sIndexClass = args[i];
				break;
			case 5:
				sIndexTrain = args[i];
				break;
			case 6:
				sThresholdTrain = args[i];
				break;
			default:
				System.err.println("Internal error. Option " + option + " is not implemented");
				System.exit(1);
				break;
			}
		}
		// Check if ARFF path exists
		File arffFile = new File(arffPath);
		if (!arffFile.exists()) {
			System.out.println("PCAP path '" + arffPath + "' does not exist");
			System.exit(1);
		}
		// Get learning algorithm
		Classifier learner = null;
		// Check that given learner exists
		int learnerOption = -1;
		if (learnerOptions.containsKey(sLearner)) {
			learnerOption = learnerOptions.get(sLearner);
		} else {
			System.out.println("Learner " + sLearner + " does not exist");
			printHelp();
			System.exit(1);
		}
		// Set learner corresponding to option
		switch (learnerOption) {
		case 0:
			learner = new AdaGrad();
			break;
		case 1:
			learner = new SGD();
			break;
		case 2:
			learner = new RandomRules();
			break;
		case 3:
			learner = new AMRulesRegressor();
			break;
		case 4:
			learner = new AdaptiveNodePredictor();
			break;
		case 5:
			learner = new FadingTargetMean();
			break;
		case 6:
			learner = new LowPassFilteredLearner();
			break;
		case 7:
			learner = new Perceptron();
			break;
		case 8:
			learner = new TargetMean();
			break;
		case 9:
			learner = new RandomAMRules();
			break;
		case 10:
			learner = new FIMTDD();
			break;
		case 11:
			learner = new ORTO();
			break;
		default:
			System.err.println("Internal error. Learner " + learnerOption + " is not implemented");
			System.exit(1);
			break;
		}
		// Parse index of the column with the values to predict
		int indexClass;
		try {
			indexClass = Integer.parseInt(sIndexClass);
		} catch (Exception e) {
			indexClass = 1;
			System.out.println("Error parsing index class '" + sIndexClass
					+ "' to integer. Using by default the first column as values to predict.");
		}
		// Parse index of the column that identifies instances for training
		int indexTrain;
		try {
			indexTrain = Integer.parseInt(sIndexTrain);
		} catch (Exception e) {
			indexTrain = -1;
			System.out.println("Error parsing index train '" + sIndexTrain
					+ "' to integer. Using by default the last column as identifier of instances for training.");
		}
		// Parse threshold that identifies instances for training
		int thresholdTrain;
		try {
			thresholdTrain = Integer.parseInt(sThresholdTrain);
		} catch (Exception e) {
			thresholdTrain = 0;
			System.out.println("Error parsing threshold train '" + sThresholdTrain
					+ "' to integer. Using by default 0 as threshold of instances for training.");
		}
		// Read ARFF file as a stream
		ArffFileStream stream = new ArffFileStream(arffPath, indexClass);
		stream.prepareForUse();
		//
		InstancesHeader learnerHeader = stream.getHeader();
		if (indexTrain == -1) {
			indexTrain = learnerHeader.numAttributes();
		}
		learnerHeader.deleteAttributeAt(indexTrain);
		learner.prepareForUse();
		learner.setModelContext(learnerHeader);
		// Evaluate learner
		double sumOfErrors = 0;
		double sumOfSquareErrors = 0;
		int countTrainSamples = 0;
		int countTestSamples = 0;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < 100; i++) {
//		while (stream.hasMoreInstances()) {
			// Obtain instance
			Instance instance = stream.nextInstance().getData();
			// Check if the instance is for prediction or training
			if (instance.value(indexTrain - 1) == 0) {
				// Remove column that indicates training
				instance.deleteAttributeAt(indexTrain - 1);
//				// Predict
				double predictedValue = learner.getPredictionForInstance(instance).getVote(0, 0);
//				double predictedValue = Math.abs(learner.getPredictionForInstance(instance).getVote(0, 0));
//				// Get real value
				double actualValue = instance.classValue();
//				// Compute error between predicted and actual value
//				double error = Math.abs(predictedValue - actualValue);
//				// Add error to the sum
//				sumOfErrors += error;
//				sumOfSquareErrors += Math.pow(error, 2);
//				// Count test samples
				countTestSamples++;
//				// Compute MAE
//				double mae = sumOfErrors / countTestSamples;
//				double rmse = Math.sqrt(sumOfSquareErrors / countTestSamples);
//				// Generate report
				result.append(countTestSamples).append(",");
				result.append(countTrainSamples).append(",");
				result.append(predictedValue).append(",");
				result.append(actualValue).append(",");
//				result.append(error).append(",");
//				result.append(mae).append(",");
//				result.append(rmse).append("\n");
				result.append("\n");
			} else {
				// Remove column that indicates training
				instance.deleteAttributeAt(indexTrain - 1);
				// Check threshold
				if (instance.classValue() > thresholdTrain) {
					learner.trainOnInstance(instance);
					countTrainSamples++;
				}
			}
		}
		System.out.println(result.toString());

		// // Check if output path exists
		// File outFile = new File(outPath);
		// if (outFile.exists()) {
		// outFile.delete();
		// }
		// // Create CSV file writer
		// FileOutputStream output;
		// try {
		// output = new FileOutputStream(outFile);
		// // output.write(report.toString().getBytes());
		// output.close();
		// } catch (FileNotFoundException e1) {
		// System.err.println("Internal error. File '" + outFile.getAbsolutePath() + "'
		// does not exist");
		// } catch (IOException e) {
		// System.err.println("Internal error. Exception thrown when writing on the file
		// '"
		// + outFile.getAbsolutePath() + "'");
		// }
	}

	public void run() {

	}

	/**
	 * Prints help
	 */
	private static void printHelp() {
		System.out.println("");
		System.out.println("=========");
		System.out.println("NELLY-MOA");
		System.out.println("=========");
		System.out.println("Options:");
		System.out.println("  --help\tDisplay this help");
		System.out.println("  --arff\tFile that contains ...");
		System.out.println("  --out\t\tFile to output the results in CSV format");
		System.out.println("  --learner\tRegressor model ...");
	}

}
