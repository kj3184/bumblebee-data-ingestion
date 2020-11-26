//import org.apache.spark.SparkConf
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.streaming._
//
//object SparkStreamTesting {
//
//  def main(args: Array[String]): Unit = {
////    val spark = SparkSession.builder().appName("NetworkWordCount").master("local[*]").getOrCreate();
//
//    val conf = new SparkConf().setMaster("local[4]").setAppName("NetworkWordCount");
//
//
//    val ssc = new StreamingContext(conf, Seconds(5));
//
//    ssc.sparkContext.setLogLevel("ERROR")
//
//    val lines = ssc.socketTextStream("localhost", 9999);
//
//    val lines2 = ssc.socketTextStream("localhost", 9900);
//
//    val wlines2=lines2.flatMap(_.split(" ")).map(word => (word, 1)).reduceByKey(_+_);
//
//    val words = lines.flatMap(_.split(" "));
//
//    val pairs = words.map(word => (word, 1));
//
//    val wordCounts = pairs.reduceByKey(_ + _);
//
//    wordCounts.print(30);
//
//    wlines2.print(30)
//
//    ssc.start();
//    ssc.awaitTermination();
//  }
//
//}
