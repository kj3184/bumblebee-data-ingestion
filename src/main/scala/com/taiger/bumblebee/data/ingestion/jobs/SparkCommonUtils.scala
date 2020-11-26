package com.taiger.bumblebee.data.ingestion.jobs

object SparkCommonUtils {

  import org.apache.spark.sql.SparkSession
  import org.apache.spark.SparkContext
  import org.apache.spark.SparkConf

  //Directory where the data files exist.
  val datadir = "inputfile"

//val input = "inputfile/Consolidated_R2_20190327.csv"//args(0)
//val output = "inputfile/json/"//args(1)


  val appName="DataIngestion-instance"
  val sparkMasterURL = "local[2]"

  //Temp dir required for Spark SQL
  val tempDir= "inputfile/temp"

  var spSession:SparkSession = null
  var spContext:SparkContext = null

  //Initialization. Runs when object is created
  {
    //Create spark configuration object
    val conf = new SparkConf()
      .setAppName(appName)
      .setMaster(sparkMasterURL)
      .set("spark.executor.memory","2g")
      .set("spark.sql.shuffle.partitions","2")

    //Get or create a spark context. Creates a new instance if not exists
    spContext = SparkContext.getOrCreate(conf)

    //Create a spark SQL session
    spSession = SparkSession
      .builder()
      .appName(appName)
      .master(sparkMasterURL)
      .config("spark.sql.warehouse.dir", tempDir)
      .getOrCreate()

  }

}
