package com.taiger.bumblebee.data.ingestion.jobs

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.feature._
import org.apache.spark.sql.functions.{col, current_date,expr}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

object ArtefactTypeModelTraining {

  final val VECTOR_SIZE = 100;

  val logger = LoggerFactory.getLogger(ArtefactTypeModelTraining.getClass);

  def classificationBasedOnType(session: SparkSession, dataFrame: DataFrame): DataFrame = {

    /**
     * Loading Object types from existing files for object types
     */
    val artefactTypeHeader = new StructType(Array(
      StructField("object_type", StringType, false),
      StructField("artefact_type",StringType,false),
      StructField("regular_cleaning", StringType, false),
      StructField("conservation_cleaning", IntegerType, false)
    ))
    val artefactTypeList = session.read
      .options(Map("inferSchema" -> "false", "delimiter" -> ",", "header" -> "true", "multiline" -> "false"))
      .format("com.databricks.spark.csv")
      .schema(artefactTypeHeader)
      .csv("./data/ObjectType_Master.csv");

    /**
     * verify the objecttype list
     */
    logger.info("displaying 10 artefact types")
    artefactTypeList.show(10)
    /**
     * Filter DataFrame with only analysis column
     */
    val dfObjectTypes = dataFrame.select("accession_no_csv", "object_work_type")
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
//    dfObjectTypes.groupBy(col("object_work_type"))
//      .count()
//      .orderBy(col("count").desc)
//      .show(50);
    /**
     * Join Object type List
     */

    val labeledObjectWorkTypes = dfObjectTypes.join(artefactTypeList, col("object_work_type")
      .contains(col("object_type"))).repartition(2)

    labeledObjectWorkTypes.show(5)

    /**
     * convert creation_place_original_location into locationText array.
     */
    val regexTokenizer = new RegexTokenizer();
    regexTokenizer.setInputCol("object_work_type");
    regexTokenizer.setOutputCol("ObjectTypeText");
    regexTokenizer.setPattern("\\W") //text to be predict

//        val tokenized = regexTokenizer.transform(labeledObjectWorkTypes);
//        tokenized.show(5)

    /**
     * default set to true and all token become lower case
     */
    regexTokenizer.setToLowercase(true);

    val add_stopwords = Array("NA",  "(", ")", "[","]")
    val stopwordsRemover = new StopWordsRemover();
    stopwordsRemover.setStopWords(add_stopwords);
    stopwordsRemover.setInputCol("ObjectTypeText").setOutputCol("filteredObjectTypeText"); //filtered text

    /**
     * configure labels
     */
    val stringIndex = new StringIndexer()
//      .setInputCol("object_type")
      .setInputCol("artefact_type")
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
//    pipeline.setStages(Array(stringIndex, regexTokenizer, word2Vec, mlpc, labelConverter));

    pipeline.setStages(Array(stringIndex, regexTokenizer, stopwordsRemover, word2Vec, mlpc, labelConverter));

    /**
     * random split the data
     */

      labeledObjectWorkTypes.show(800)

    val Array(trainingData, testData) = labeledObjectWorkTypes.randomSplit(Array(0.3, 0.7),112);
//    println(trainingData.count());
//    println(testData.count());


    val pipelinefit = pipeline.fit(trainingData)

//    pipeline.

//    pipelinefit.save("inputfile/model/");// => input text -> API input json -> model -> come out recommand
    /**
     * evaluate the result
     */

    val dataset = pipelinefit.transform(testData);
    dataset.show(20);

//    val evaluator = new MulticlassClassificationEvaluator().setLabelCol("objectTypeLabel").setPredictionCol("prediction").setMetricName("accuracy")
//    val predictionAccuracy = evaluator.evaluate(dataset);
//    println("Testing Accuracy is %2.4f".format(predictionAccuracy * 100) + "%")

    /**
     * Model Training and Evaluation
     */
    val resultTransfer= pipelinefit.transform(dfObjectTypes);
    resultTransfer.show(50)

    val resultTransfer1 = resultTransfer.join(artefactTypeList, col("predictionLabel")
      .contains(col("artefact_type"))).repartition(2)

    resultTransfer1.show(10)

//    val resultTransfer1= pipelinefit.transform(labeledObjectWorkTypes);
    val result3=resultTransfer1.drop("ObjectTypeText","filteredObjectTypeText","features","rawPrediction","probability","prediction","predictionLabel","object_type")
//    val result33=result3.withColumn("next_cleaning_on",current_date())
//    result3.show(10)
    val result4=result3.withColumn("regular_cleaning2", result3.col("regular_cleaning").cast(IntegerType))
      .withColumn("conservation_cleaning2",result3.col("conservation_cleaning").cast(IntegerType))

    val resultFinal=result4.withColumn("current_date",current_date()).withColumn("next_reg_cleaning_on",
      expr("date_add(current_date, regular_cleaning2)")).withColumn("next_conservation_cleaning_on",
      expr("date_add(current_date, conservation_cleaning2)")).drop("regular_cleaning","regular_cleaning2",
      "conservation_cleaning","conservation_cleaning2","current_date")


    resultFinal.show(20)


    return resultFinal;

  }




  }
