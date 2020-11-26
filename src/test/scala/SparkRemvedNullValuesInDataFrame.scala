//import org.apache.spark.sql.SparkSession
//
//object SparkRemvedNullValuesInDataFrame {
//
//  def main(args: Array[String]): Unit = {
//
//    var spark = SparkSession.builder().appName("Spark remove null values").master("local[*]").getOrCreate()
//
//    var file = "./Data/RawData.csv"
//    var properties = Map("header" -> "true", "inferSchema" -> "true")
//    var df = spark.read
//      .options(properties)
//      .format("com.databricks.spark.csv")
//      .csv(file);
//    val nonNullDf = df.na.fill("");
//    nonNullDf.show(60)
//
//    // Convert DataFrame to Json Object
//
//    nonNullDf.write.csv("./Data/output.csv")
//
//
//
//  }
//
//}
