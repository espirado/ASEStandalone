/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umcg.genetica.math.stats;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.RankingAlgorithm;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import umcg.genetica.math.matrix.DoubleMatrixDataset;
import umcg.genetica.util.RankArray;


/**
 *
 * @author Harm Jan & Marc Jan Bonder
 */
public class QuantileNormalization {
    private static final RankingAlgorithm COV_RANKER_TIE = new NaturalRanking(NaNStrategy.FAILED, TiesStrategy.AVERAGE);
    /**
     * Quantile normalize a double[][] double[probes][sample]
     *
     * @param rawData matrix containing expression/methylation data
     */
    public static void quantilenormalize(double[][] rawData) {
        System.out.println("\nPerforming quantile normalization:");
        //Calculate the average expression, when per sample all raw expression levels have been ordered:

        org.apache.commons.math3.stat.correlation.SpearmansCorrelation spearman = new org.apache.commons.math3.stat.correlation.SpearmansCorrelation();
        
        int probeCount = rawData.length;
        int sampleCount = rawData[probeCount - 1].length;

        double[] rankedMean = new double[probeCount];
        for (int sampleID = 0; sampleID < sampleCount; sampleID++) {
            double[] x = new double[probeCount];

            for (int probeID = 0; probeID < probeCount; probeID++) {
                x[probeID] = rawData[probeID][sampleID];
            }
            java.util.Arrays.sort(x);
            for (int probeID = 0; probeID < probeCount; probeID++) {
                rankedMean[probeID] += x[probeID];
            }
        }
        
        for (int probeID = 0; probeID < probeCount; probeID++) {
            rankedMean[probeID] /= (double) sampleCount;
        }
        
        double[] rankedMeanClasses =  new double[probeCount-1];
        
        for (int probeID = 0; probeID < (probeCount-1); probeID++) {
            rankedMeanClasses[probeID] = ((rankedMean[probeID]+rankedMean[probeID+1])/2);
        }

        //Iterate through each sample:
        for (int s = 0; s < sampleCount; s++) {
            double[] probes = new double[probeCount];
            for (int p = 0; p < probeCount; p++) {
                probes[p] = rawData[p][s];
            }
            double[] probesRanked = COV_RANKER_TIE.rank(probes);
            
            double[] probesQuantileNormalized = new double[probeCount];
            for (int p = 0; p < probeCount; p++) {
                                
                if((probesRanked[p]%1)!=0){
                    probesQuantileNormalized[p] = rankedMeanClasses[(int)Math.floor((probesRanked[p]-1))];
                } else {
                    probesQuantileNormalized[p] = rankedMean[(int) (probesRanked[p]-1)];
                }
                
                rawData[p][s] = probesQuantileNormalized[p];
            }
//            double[] probesRankedAfterQQNorm = rda.rank(probesQuantileNormalized, false);
            
            System.out.println("Normalized sample:\t" + (s+1) + "\tPearson correlation original data and ranked data:\t" + JSci.maths.ArrayMath.correlation(probes, probesRanked) + "\tSpearman correlation original data and quantile normalized data:\t"+spearman.correlation(probes, probesQuantileNormalized));
        }
    }

    /**
     * Quantile normalize data where missing values are allowed. the missing
     * values are replaced with the median or mean of the other probes in the
     * sample If chosen keep median value instead of the NA value.
     *
     * NB: For now NA is defined as "-999"!!
     *
     * @param rawData matrix containing expression/methylation data
     * @param useMean use mean for guessing the NA value if false use median
     * @param retainNA retain the NA values, put NA values back after
     * normalization
     */
    public static void QuantileNormAdressingNaValuesBeforeQN(double[][] rawData, boolean useMedian, boolean retainNA) {
        boolean[][] wasNA = new boolean[rawData.length][rawData[1].length];

        for (int s = 0; s < rawData[1].length; ++s) {
            
            ArrayDoubleList nonNAvalues = new ArrayDoubleList();
            
            boolean needsReplacement = false;

            for (int p = 0; p < rawData.length; ++p) {
                if (rawData[p][s] == -999) {
                    needsReplacement = true;
                    wasNA[p][s] = true;
                } else {
                    wasNA[p][s] = false;
                    nonNAvalues.add(rawData[p][s]);
                }
            }

            if (needsReplacement) {
                double replacementValue;
                if (useMedian) {
                    replacementValue = JSci.maths.ArrayMath.median(nonNAvalues.toArray(new double[0]));
                } else {
                    replacementValue = JSci.maths.ArrayMath.mean(nonNAvalues.toArray(new double[0]));

                }

                for (int p = 0; p < rawData.length; ++p) {
                    if (wasNA[p][s]) {
                        rawData[p][s] = replacementValue;
                    }
                }
            }
        }

        quantilenormalize(rawData);

        if (retainNA) {
            for (int s = 0; s < rawData[1].length; ++s) {
                for (int p = 0; p < rawData.length; ++p) {
                    if (wasNA[p][s]) {
                        rawData[p][s] = -999;
                    }
                }
            }
        }

    }

    /**
     * Quantile normalize data where missing values are allowed Do QN on all
     * non-missing values, after which the missing values are replaced and a
     * second QN is performed.
     *
     *
     * @param dataset
     * @param retainNA retain the NA values, put NA values back after
     * normalization
     * @param useRow use row to guess the median expression value, instead of
     * column
     */
    public static void QuantileNormAdressingNaValuesAfterInitialQN(DoubleMatrixDataset<String, String> dataset, boolean retainNA, boolean useRow, boolean keepZero) {
        //Quantile normalisation, allowing for missing values:
        //ToDo: Can optimeze for alot of missing values. Tempory remove rows. Should speed it up and get better results.
        
        System.out.print("Pre-treating missing values:");
        ArrayList<ArrayDoubleList> dataForPretreatment = new ArrayList<ArrayDoubleList>();
        
        int maxNonNAvalues = Integer.MIN_VALUE;
        
        for (int s = 0; s < dataset.nrCols; s++) {
            
            ArrayDoubleList nonNAvalues = new ArrayDoubleList();  
            
            for (int p = 0; p < dataset.nrRows; ++p) {
                if (!Double.isNaN(dataset.rawData[p][s])) {
                    nonNAvalues.add(dataset.rawData[p][s]);
                }
            }
            
            if(nonNAvalues.size()>maxNonNAvalues){
                maxNonNAvalues = nonNAvalues.size();
            }
            
            dataForPretreatment.add(nonNAvalues);
        }
        
        double[][] dataSorted = new double[maxNonNAvalues][dataset.nrCols];

        for (int s = 0; s < dataset.nrCols; s++) {
            double vals[] = new double[maxNonNAvalues];
            
            double meanPerSample = 0;
            
            if(dataForPretreatment.get(s).size()>0 && !keepZero){
                meanPerSample = JSci.maths.ArrayMath.mean(dataForPretreatment.get(s).toArray(new double[0]));
            }
            
            for (int p = 0; p < maxNonNAvalues; ++p) {
                if (dataForPretreatment.get(s).size()>p) {
                    vals[p] = dataForPretreatment.get(s).get(p);
                } else {
                    vals[p]=meanPerSample;
                }
            }
            
            Arrays.sort(vals);
            
            for (int p = 0; p < vals.length; p++) {
                dataSorted[p][s] = vals[p];
            }
        }
        
        double[] dist = new double[maxNonNAvalues];
        for (int p = 0; p < maxNonNAvalues; p++) {            
            dist[p] = JSci.maths.ArrayMath.mean(dataSorted[p]);
        }

        dataSorted = null;
        
        System.out.println("done");
        
        System.out.println("Quantile normalization round (1)");        
        org.apache.commons.math3.stat.correlation.SpearmansCorrelation spearman = new org.apache.commons.math3.stat.correlation.SpearmansCorrelation();
        
        for (int s = 0; s < dataset.nrCols; s++) {

            double[] vals1 = new double[dataForPretreatment.get(s).size()];
            
            RankArray rda = new RankArray();
            
            double[] valsRanked = rda.rank(dataForPretreatment.get(s).toArray(new double[0]), false);

            for (int v = 0; v < vals1.length; v++) {
                double quantile = (valsRanked[v]) / ((double) vals1.length);
                int distIndex = (int) ((quantile * (double) maxNonNAvalues) - 1);
                vals1[v] = dist[distIndex+1];
            }
            
            System.out.println("Normalized sample:\t" + dataset.colObjects.get(s) + "\t" + s + "\tCorrelation original data and ranked data:\t" + JSci.maths.ArrayMath.correlation(dataForPretreatment.get(s).toArray(new double[0]), valsRanked) + "\tCorrelation original data and quantile normalized data:\t" + JSci.maths.ArrayMath.correlation(dataForPretreatment.get(s).toArray(new double[0]), vals1)+"\t"+spearman.correlation(dataForPretreatment.get(s).toArray(new double[0]), vals1));

            int itr = 0;
            for (int p = 0; p < dataset.nrRows; p++) {
                if (!Double.isNaN(dataset.rawData[p][s])) {
                    dataset.rawData[p][s] = vals1[itr];
                    itr++;
                }
            }
        }
        
        //Replace missing values:
        if (!retainNA) {
            if (useRow) {
                for (int p = 0; p < dataset.nrRows; p++) {
                    double valSum = 0;
                    int nr = 0;
                    boolean foundNA = false;
                    for (int s = 0; s < dataset.nrCols; s++) {
                        if (!Double.isNaN(dataset.rawData[p][s])) {
                            valSum += dataset.rawData[p][s];
                            nr++;
                        } else {
                            foundNA = true;
                        }
                    }
                    if(foundNA){
                        double mean = valSum / nr;
                        for (int s = 0; s < dataset.nrCols; s++) {
                            if (Double.isNaN(dataset.rawData[p][s])) {
                                dataset.rawData[p][s] = mean;
                            }
                        }
                    }
                }
            } else {
                for (int s = 0; s < dataset.nrCols; s++) {
                    double valSum = 0;
                    int nr = 0;
                    boolean foundNA = false;
                    for (int p = 0; p < dataset.nrRows; p++) {
                        if (!Double.isNaN(dataset.rawData[p][s])) {
                            valSum += dataset.rawData[p][s];
                            nr++;
                        } else {
                            foundNA = true;
                        }
                    }
                    if(foundNA){
                        double mean = valSum / nr;
                        for (int p = 0; p < dataset.nrRows; p++) {
                            if (Double.isNaN(dataset.rawData[p][s])) {
                                dataset.rawData[p][s] = mean;
                            }
                        }
                    }
                }
            }

            System.out.println("Quantile normalization round 2");
            quantilenormalize(dataset.rawData);
        }

    }
}
