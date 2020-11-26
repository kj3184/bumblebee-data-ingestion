package com.taiger.bumblebee.data.ingestion.jobs

// $example on$
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.Row

// $example off$
import org.apache.spark.sql.SparkSession

object StopWordRemoverExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName("CSV to json data ingestion").master("local").getOrCreate()

    val documentDF = spark.createDataFrame(Seq(
      "Hi I heard about Spark".split(" "),
      "I wish Java could use case classes".split(" "),
      "Logistic regression models are neat".split(" ")
    ).map(Tuple1.apply)).toDF("text")

    documentDF.show(3)
    // Learn a mapping from words to Vectors.
    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(3)
      .setMinCount(0)
    val model = word2Vec.fit(documentDF)

    val result = model.transform(documentDF)
    result.collect().foreach { case Row(text: Seq[_], features: Vector) =>
      println(s"Text: [${text.mkString(", ")}] => \nVector: $features\n") }
    // $example off$

    spark.stop()
  }
}
// scalastyle:on println
