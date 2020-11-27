package com.taiger.bumblebee.data.ingestion.jobs

/*
Author : Kunal Jadhav
 */

import java.io._
import java.net.URLEncoder
import java.util.Date

import com.google.gson.GsonBuilder
import jsonclass.{Metadata, OutputCsv, Tag}
import org.apache.commons.io.FileUtils
import org.apache.spark.sql
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Encoders, SparkSession}
import org.slf4j.LoggerFactory

object DataIngestionSolution {

  val logger = LoggerFactory.getLogger(DataIngestionSolution.getClass);
  val metadataSchema = Encoders.product[Metadata].schema;


  def main(args: Array[String]): Unit = {

    val inputFile = args(0) //"inputfile/Consolidated_R2_20190327.csv" //args(0)
    val outputJSONLocation = args(1)//"inputfile/json/" //args(1)
    val outputCsvLocation = args(2)//"inputfile/csv/" //args(1)

    logger.info("input file name: {}", inputFile)
    logger.info("json output location {}", outputJSONLocation)
    logger.info("csv output location {}", outputCsvLocation)

    logger.info("Initializing connection with spark server...")

    val session = connectToSpark()
    val unstructuredCSVDF = readingCSVfile(session, inputFile)
    val museumDataDF = processCSVFile(unstructuredCSVDF)


    val dataBasedOnTypeDF = ArtefactTypeModelTraining.classificationBasedOnType(session, museumDataDF)
//    dataBasedOnTypeDF.show(800)

    transformOriginalCSVtoJson(museumDataDF, outputJSONLocation,outputCsvLocation)

    transformArtefactTypeDFtoCSV(dataBasedOnTypeDF,outputJSONLocation,outputCsvLocation)

  }

  def connectToSpark(): SparkSession = {
    val session = SparkSession.builder().appName("CSV to json data ingestion").master("local").getOrCreate()
    return session
  }

  def readingCSVfile(session: SparkSession, input: String): sql.DataFrame = {
    val unstructuredCSVDF = session.read.format("com.databricks.spark.csv")
      .options(Map("inferSchema" -> "false", "delimiter" -> ",", "header" -> "true", "multiline" -> "true"))
      .schema(metadataSchema)
      .csv(input)
    logger.info("DataFrame schema...")
    unstructuredCSVDF.printSchema();
    logger.info("DataFrame size {}", unstructuredCSVDF.count());
    return unstructuredCSVDF
  }

  def processCSVFile(df: sql.DataFrame): DataFrame = {
    /**
     * Remove null string from raw files
     */
    //    val nonNullDf = df.na.fill("");

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
      .agg(trim(concat_ws(" ", collect_set(regexp_replace(trim(col("Image")), "\\?{2,}", "")))).as("Image")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("object_work_type")), "\\?{2,}", "")))).as("object_work_type")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("title_text")), "\\?{2,}", "")))).as("title_text")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("preference")), "\\?{2,}", "")))).as("preference")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("title_language")), "\\?{2,}", "")))).as("title_language")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("creator_2")), "\\?{2,}", "")))).as("creator_2")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("creator_1")), "\\?{2,}", "")))).as("creator_1")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("creator_role")), "\\?{2,}", "")))).as("creator_role")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("creation_date")), "\\?{2,}", "")))).as("creation_date")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("creation_place_original_location")), "\\?{2,}", "")))).as("creation_place_original_location")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("styles_periods_indexing_terms")), "\\?{2,}", "")))).as("styles_periods_indexing_terms")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("inscriptions")), "\\?{2,}", "")))).as("inscriptions")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("inscription_language")), "\\?{2,}", "")))).as("inscription_language")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("scale_type")), "\\?{2,}", "")))).as("scale_type")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("shape")), "\\?{2,}", "")))).as("shape")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("materials_name")), "\\?{2,}", "")))).as("materials_name")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("techniques_name")), "\\?{2,}", "")))).as("techniques_name")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("object_colour")), "\\?{2,}", "")))).as("object_colour")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("edition_description")), "\\?{2,}", "")))).as("edition_description")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("physical_appearance")), "\\?{2,}", "")))).as("physical_appearance")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("subject_terms_1")), "\\?{2,}", "")))).as("subject_terms_1")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("subject_terms_2")), "\\?{2,}", "")))).as("subject_terms_2")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("subject_terms_3")), "\\?{2,}", "")))).as("subject_terms_3")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("subject_terms_4")), "\\?{2,}", "")))).as("subject_terms_4")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_1")), "\\?{2,}", "")))).as("context_1")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_2")), "\\?{2,}", "")))).as("context_2")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_3")), "\\?{2,}", "")))).as("context_3")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_4")), "\\?{2,}", "")))).as("context_4")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_5")), "\\?{2,}", "")))).as("context_5")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_6")), "\\?{2,}", "")))).as("context_6")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_7")), "\\?{2,}", "")))).as("context_7")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_8")), "\\?{2,}", "")))).as("context_8")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_9")), "\\?{2,}", "")))).as("context_9")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_10")), "\\?{2,}", "")))).as("context_10")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_11")), "\\?{2,}", "")))).as("context_11")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_12")), "\\?{2,}", "")))).as("context_12")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_13")), "\\?{2,}", "")))).as("context_13")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_14")), "\\?{2,}", "")))).as("context_14")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_15")), "\\?{2,}", "")))).as("context_15")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_16")), "\\?{2,}", "")))).as("context_16")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_17")), "\\?{2,}", "")))).as("context_17")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_18")), "\\?{2,}", "")))).as("context_18")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_19")), "\\?{2,}", "")))).as("context_19")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_20")), "\\?{2,}", "")))).as("context_20")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_21")), "\\?{2,}", "")))).as("context_21")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_22")), "\\?{2,}", "")))).as("context_22")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_23")), "\\?{2,}", "")))).as("context_23")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("context_24")), "\\?{2,}", "")))).as("context_24")
        , trim(concat_ws(" ", collect_set(regexp_replace(trim(col("sgcool_label_text")), "\\?{2,}", "")))).as("sgcool_label_text"))
      .toDF().orderBy(desc("accession_no_csv"));

    val structuredCSVDF = DfMerged.na.replace(df.columns, Map("NA" -> null));
//    structuredCSVDF.show();
    return structuredCSVDF;

  }


  val uuid = udf(() => java.util.UUID.randomUUID().toString)

  def transformOriginalCSVtoJson(df: sql.DataFrame, output: String, outputCsvPath: String): Unit = {

    logger.info("in method transformCSVJson")

    val filterResult = df.filter(col("accession_no_csv").rlike("^[\\d]{4}-[\\d]*"))

    val outputCsvFolder = new File(outputCsvPath);
    if (outputCsvFolder.exists())
      FileUtils.deleteDirectory(outputCsvFolder);

    filterResult.withColumn("id", uuid())
      .select(
        col("id"),
        col("accession_no_csv"),
        col("object_work_type"),
        col("title_text")
      )
      .coalesce(1).write
      .format("csv")
      .option("header", "true")
      .save(outputCsvPath)


    renameCsvOutput(outputCsvPath, "csv", "output.csv")

    /**
     * Convert Row to Object
     */
    val result = filterResult.as(Encoders.product[Metadata]).collect()

    //    val outputObjectList = new util.ArrayList[OutputCsv]();
    val gsonBuilderObj = new GsonBuilder().setDateFormat("yyyyMMddHHmmss").setPrettyPrinting().create();
    result.foreach(meta => {
      val fileName = meta.accession_no_csv.trim().replaceAll("[\\n,\\r]", "__") + ".json";
      val outputObj = new OutputCsv(
        meta.accession_no_csv,
        meta.title_text,
        "Bumblebee",
        output,
        gsonBuilderObj.toJson(meta).replace("\"", "").replace(",", "\n").replace("{", "").replace("}", ""),
        null,
        new Date(),
        null,
        0,
        false,
        Array(),
        meta,
        null,
        null,
        null,
        null,
        null,
        null,
        new Date(),
        0,
        null,
        new Tag(null),
        null,
        null
      );
      //      outputObjectList.add(outputObj);
      var content = gsonBuilderObj.toJson(outputObj);
      writeToFile(output, fileName, content);
      //      KafkaUtils.messageProducer("books-testing-messages-broadcast",UUID.randomUUID().toString, outputObj)
      //pushToElasticSearch(content, meta.accession_no_csv.trim().replaceAll("[\\n,\\r]", "__"));
    })


    //    println(outputObjectList.size());

  }

  /**
   *
   * @param path     the file store path
   * @param filename file name
   * @param content  file string content
   */
  def writeToFile(path: String, filename: String, content: String) = {
    val dir = new File(path);
    if (dir.exists()) {
      dir.delete();
      //      dir.mkdir();
    }
    dir.mkdir();
    val file = new File(path + "/" + filename);
    try {
      logger.info("Writing to file {}", filename);
      val fw = new FileWriter(file.getAbsoluteFile());
      val bw = new BufferedWriter(fw);
      bw.write(content);
      bw.close();
    }
    catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Had an IOException trying to read that file")
    }

  }

  def renameCsvOutput(path: String, fileType: String, newName: String): Unit = {
    val directory: File = new File(path);
    assert(directory.isDirectory)

    val csvFiles = directory.listFiles.filter(d =>
      d.isFile && d.getName.endsWith(fileType)
    )
    csvFiles.foreach(file => {
      file.renameTo(new File(path + "/" + newName))
      println(file.getAbsolutePath)
    })
  }

  def pushToElasticSearch(content: String, id: String): Unit = {
    val url = "http://localhost:9200/books/_doc/" + URLEncoder.encode(id, "UTF-8")
    requests.put(url, data = content, headers = Map("content-type" -> "application/json"));
  }


  def transformArtefactTypeDFtoCSV(df: sql.DataFrame, output: String, outputCsvPath: String): Unit = {

    logger.info("in method transformCSVJson")

    val filterResult = df.filter(col("accession_no_csv").rlike("^[\\d]{4}-[\\d]*"))

    val outputCsvFolder = new File(outputCsvPath);
    if (outputCsvFolder.exists())
      FileUtils.deleteDirectory(outputCsvFolder);

    filterResult.withColumn("id", uuid())
      .select(
        col("id"),
        col("accession_no_csv"),
        col("object_work_type"),
        col("artefact_type"),
        col("next_reg_cleaning_on"),
        col("next_conservation_cleaning_on")
      )

      .coalesce(1).write
      .format("csv")
      .option("header", "true")
      .save(outputCsvPath)


    renameCsvOutput(outputCsvPath, "csv", "output_artefacttype.csv")
  }

}