package com.taiger.bumblebee.data.ingestion.jobs

import jsonclass.Metadata
import org.apache.spark.sql
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, collect_list, concat_ws, last, monotonically_increasing_id, trim}
import org.apache.spark.sql.{DataFrame, Encoders, SparkSession}
import org.slf4j.LoggerFactory


object CSVDataLoading {

  val logger = LoggerFactory.getLogger(CSVDataLoading.getClass);
  val spContext = SparkCommonUtils.spContext
  val spSessiom=SparkCommonUtils.spSession
  val datadir = SparkCommonUtils.datadir
  val medataSchema = Encoders.product[Metadata].schema;

  def main(args: Array[String]): Unit = {

    val input = datadir+"/Consolidated_R2_20190327.csv"//args(0)
    val output = datadir+"/json/"//args(1)

    logger.info("input file name: {}", input)
    logger.info("output path {}", output)

    val df = readingCSVfile(spSessiom, input)
    val outputDf = processCSVFile(df)

    outputDf.show(800)
  }

  def readingCSVfile(session: SparkSession, input: String): sql.DataFrame = {
    val df = session.read.format("com.databricks.spark.csv")
      .options(Map("inferSchema" -> "false", "delimiter" -> ",", "header" -> "true", "multiline" -> "true"))
      .schema(medataSchema)
      .csv(input)
    logger.info("DataFrame schema...")
    df.printSchema();
    logger.info("DataFrame size {}", df.count());
    return df
  }

  def processCSVFile(df: sql.DataFrame): DataFrame = {

    /**
     * fill with last good observation
     */
    val dataWithIndex = df.withColumn("idx", monotonically_increasing_id());
    val partitionWindow = Window.orderBy("idx")
    val Df2 = dataWithIndex.withColumn("accession_no_csv", last("accession_no_csv", true) over (partitionWindow))


    /**
     * Merged the records with same id: accession_no_csv
     */
    val DfMerged = Df2.filter(col("accession_no_csv").isNotNull).groupBy("accession_no_csv")
      .agg(trim(concat_ws(" ", collect_list("Image"))).as("Image")
        , trim(concat_ws(" ", collect_list("object_work_type"))).as("object_work_type")
        , trim(concat_ws(" ", collect_list("title_text"))).as("title_text")
        , trim(concat_ws(" ", collect_list("preference"))).as("preference")
        , trim(concat_ws(" ", collect_list("title_language"))).as("title_language")
        , trim(concat_ws(" ", collect_list("creator_2"))).as("creator_2")
        , trim(concat_ws(" ", collect_list("creator_1"))).as("creator_1")
        , trim(concat_ws(" ", collect_list("creator_role"))).as("creator_role")
        , trim(concat_ws(" ", collect_list("creation_date"))).as("creation_date")
        , trim(concat_ws(" ", collect_list("creation_place_original_location"))).as("creation_place_original_location")
        , trim(concat_ws(" ", collect_list("styles_periods_indexing_terms"))).as("styles_periods_indexing_terms")
        , trim(concat_ws(" ", collect_list("inscriptions"))).as("inscriptions")
        , trim(concat_ws(" ", collect_list("inscription_language"))).as("inscription_language")
        , trim(concat_ws(" ", collect_list("scale_type"))).as("scale_type")
        , trim(concat_ws(" ", collect_list("shape"))).as("shape")
        , trim(concat_ws(" ", collect_list("materials_name"))).as("materials_name")
        , trim(concat_ws(" ", collect_list("techniques_name"))).as("techniques_name")
        , trim(concat_ws(" ", collect_list("object_colour"))).as("object_colour")
        , trim(concat_ws(" ", collect_list("edition_description"))).as("edition_description")
        , trim(concat_ws(" ", collect_list("physical_appearance"))).as("physical_appearance")
        , trim(concat_ws(" ", collect_list("subject_terms_1"))).as("subject_terms_1")
        , trim(concat_ws(" ", collect_list("subject_terms_2"))).as("subject_terms_2")
        , trim(concat_ws(" ", collect_list("subject_terms_3"))).as("subject_terms_3")
        , trim(concat_ws(" ", collect_list("subject_terms_4"))).as("subject_terms_4")
        , trim(concat_ws(" ", collect_list("context_1"))).as("context_1")
        , trim(concat_ws(" ", collect_list("context_2"))).as("context_2")
        , trim(concat_ws(" ", collect_list("context_3"))).as("context_3")
        , trim(concat_ws(" ", collect_list("context_4"))).as("context_4")
        , trim(concat_ws(" ", collect_list("context_5"))).as("context_5")
        , trim(concat_ws(" ", collect_list("context_6"))).as("context_6")
        , trim(concat_ws(" ", collect_list("context_7"))).as("context_7")
        , trim(concat_ws(" ", collect_list("context_8"))).as("context_8")
        , trim(concat_ws(" ", collect_list("context_9"))).as("context_9")
        , trim(concat_ws(" ", collect_list("context_10"))).as("context_10")
        , trim(concat_ws(" ", collect_list("context_11"))).as("context_11")
        , trim(concat_ws(" ", collect_list("context_12"))).as("context_12")
        , trim(concat_ws(" ", collect_list("context_13"))).as("context_13")
        , trim(concat_ws(" ", collect_list("context_14"))).as("context_14")
        , trim(concat_ws(" ", collect_list("context_15"))).as("context_15")
        , trim(concat_ws(" ", collect_list("context_16"))).as("context_16")
        , trim(concat_ws(" ", collect_list("context_17"))).as("context_17")
        , trim(concat_ws(" ", collect_list("context_18"))).as("context_18")
        , trim(concat_ws(" ", collect_list("context_19"))).as("context_19")
        , trim(concat_ws(" ", collect_list("context_20"))).as("context_20")
        , trim(concat_ws(" ", collect_list("context_21"))).as("context_21")
        , trim(concat_ws(" ", collect_list("context_22"))).as("context_22")
        , trim(concat_ws(" ", collect_list("context_23"))).as("context_23")
        , trim(concat_ws(" ", collect_list("context_24"))).as("context_24")
        , trim(concat_ws(" ", collect_list("sgcool_label_text"))).as("sgcool_label_text"))
      .toDF();

    val output2 = DfMerged.na.replace(df.columns, Map("NA" -> null));
    output2.show(100);
    return output2;
  }


}