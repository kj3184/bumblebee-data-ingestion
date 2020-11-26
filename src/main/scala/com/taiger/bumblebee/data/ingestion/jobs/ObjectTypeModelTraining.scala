package com.taiger.bumblebee.data.ingestion.jobs

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}

object ObjectTypeModelTraining {

  final val VECTOR_SIZE = 100;

  def objectTypeClassification(session: SparkSession, df: DataFrame): DataFrame = {

    /**
     * Loading Object types from existing files for object types
     */
    val objectTypeHeader = new StructType(Array {
      StructField("object_type", StringType, false)
    })
    val objectTypeList = session.read
      .format("com.databricks.spark.csv")
      .schema(objectTypeHeader)
      .csv("inputfile/ObjectType_Master.csv");

    /**
     * verify the objecttype list
     */
    objectTypeList.show(10)
    /**
     * Filter DataFrame with only analysis column
     */
    val dfObjectTypes = df.select("accession_no_csv", "object_work_type")
      .filter(col("object_work_type").isNotNull)
      .filter(col("object_work_type").notEqual(""))
      .filter(col("accession_no_csv").rlike("^[\\d]{4}-[\\d]*"));

    /**
     * verify the output
     */
    dfObjectTypes.show(5)

    /**
     * count by Object Types, but the location some has multiple words
     */
    dfObjectTypes.groupBy(col("object_work_type"))
      .count()
      .orderBy(col("count").desc)
      .show(50);
    /**
     * Join Object type List
     */

    val labeledObjectWorkTypes = dfObjectTypes.join(objectTypeList, col("object_work_type")
      .contains(col("object_type")));

    /**
     * Spark pipeline analyze
     */

    /**
     * convert creation_place_original_location into locationText array.
     */
    val regexTokenizer = new RegexTokenizer();
    regexTokenizer.setInputCol("object_work_type");
    regexTokenizer.setOutputCol("ObjectTypeText");
    regexTokenizer.setPattern("\\W") //text to be predict
    //    regexTokenizer.+("\\w+");
    /**
     * default set to true and all token become lower case
     */
    regexTokenizer.setToLowercase(true);

    //    val tokenized = regexTokenizer.transform(labeledObjectWorkTypes);
    //    tokenized.show(100)

    /**
     * display tokens before predict
     */
    //    val countTokens = udf { (words: Seq[String]) => words.length }
    //    tokenized.select("*")
    //      .withColumn("tokens", countTokens(col("locationText")))
    //      .filter(col("tokens").equalTo(1))
    //      .show(false);

    val add_stopwords = Array("visual", "works", "drinking", "vessels", "(", ")", "texts", "photographs","garments","candleholders")
    val stopwordsRemover = new StopWordsRemover();
    stopwordsRemover.setStopWords(add_stopwords);
    stopwordsRemover.setInputCol("ObjectTypeText").setOutputCol("filteredObjectTypeText"); //filtered text

    /**
     * configure labels
     */
    val stringIndex = new StringIndexer()
      .setInputCol("object_type")
      .setOutputCol("ObjectTypeLabel")
      .fit(labeledObjectWorkTypes);

    println("stringIndex.labels.size {}", stringIndex.labels.size);

    stringIndex.labels.foreach {
      str =>
        println(str)
    }

    /**
     * Create word vectors
     */
    val word2Vec = new Word2Vec().setInputCol("filteredObjectTypeText").setOutputCol("features").setVectorSize(VECTOR_SIZE).setMinCount(1)

    val layers = Array[Int](VECTOR_SIZE, 6, 5, stringIndex.labels.size);
    val mlpc = new MultilayerPerceptronClassifier()
      .setLayers(layers)
      .setBlockSize(512)
      .setSeed(1234L)
      .setMaxIter(128)
      .setFeaturesCol("features")
      .setLabelCol("ObjectTypeLabel")
      .setPredictionCol("prediction");

    /**
     * prediction labels
     */

    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictionLabel")
      .setLabels(stringIndex.labels);

    /**
     * based on Spark pipeline to extract the data
     */


    val pipeline = new Pipeline();
    pipeline.setStages(Array(stringIndex, regexTokenizer, stopwordsRemover, word2Vec, mlpc, labelConverter));

    /**
     * random split the data
     */

    val Array(trainingData, testData) = labeledObjectWorkTypes.randomSplit(Array(0.5, 0.5), 112);
    println(trainingData.count());
    println(testData.count());


    val pipelinefit = pipeline.fit(trainingData);

    pipelinefit.save("");// => input text -> API input json -> model -> come out recommand
    /**
     * evaluate the result
     */

    val dataset = pipelinefit.transform(testData);
    dataset.show(20);

    val evaluator = new MulticlassClassificationEvaluator().setLabelCol("objectTypeLabel").setPredictionCol("prediction").setMetricName("accuracy")
    val predictionAccuracy = evaluator.evaluate(dataset);
    println("Testing Accuracy is %2.4f".format(predictionAccuracy * 100) + "%")

    /**
     * Model Training and Evaluation
     */
    val resultTransfer= pipelinefit.transform(dfObjectTypes);
    resultTransfer.show(50)
    return resultTransfer;
  }



}
