package com.keene.foo_say_bar_dao

import java.text.SimpleDateFormat
import java.util.Calendar

import com.keene.core.implicits._
import com.keene.spark.utils.SimpleSpark

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

object Task extends App with SimpleSpark {


  def pairs (start: String = "2018-08-13 00:00:00") = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val cld = Calendar.getInstance
    cld setTime fmt.parse(start)

    val res = ListBuffer.newBuilder[ Long ]
    val now = fmt.parse("2018-08-14 00:00:00")

    while (now after cld.getTime ) {
      res += cld.getTime.getTime
      cld.add(Calendar.HOUR_OF_DAY, 1)
    }

    res += cld.getTime.getTime

    val result = res.result.toList
    result.indices zip (0l :: result zip result drop 1)
  }

  val ses = pairs().par

  ses.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(6))

  val a = ses.map { case (h, (start, end)) => (h, spark.sql(
    s"""
       |select count(1)
       |from antidb.search_app_click_log
       |where dt = '2018-08-08'
       |  and (event_id = 'Searchlist_Productid'
       |  or event_id = 'SearchList_Productid'
       |  or event_id = 'Searchlist_AddtoCartforfood')
       |  and page_param != '搜索:6240_6233_list' and page_param != '搜索:_0'
       |  and split(page_param, ':')[0] = '搜索' and size(split(page_param, '_')) >= 4 and size(split(event_param, '_')) >= 5
       |  and split(event_param, '_')[0] is not null and length(split(event_param, '_')[0]) > 0
       |  and size(split(page_param, ':')) > 1
       |  and click_ts >= $start and click_ts < $end
      """.stripMargin).count)
  }.toList

  val tre = 15000000l
  var total = 0l
  var i = 0
  ("" /: a) { case (n, (h, c)) =>
    val next = if (total > tre) {

      total = 0
      "@" + h
    } else {
      "_" + h
    }
    total += c

    println( i, total)
    i += 1
    n + next
  }

  /*val sqls = Map(//    "catfish_free_commission_spam_order" -> List("cps", "gdt", "jzt", "tpm"),
    //    "base_ads_click_sum_log" -> List("cps", "gdt", "jzt", "tpm"),
    //    "base_ads_order_sum_log" -> List("cps", "gdt", "jzt", "tpm")
    "base_user_behaviour_sum_log" -> List("app", "pc_m", "wx_sq")).mapValues { pts => for (pt <- pts; dt <- toNow()) yield (pt, dt) }.flatMap { case (table, partitions) => partitions map { case (pt, dt) => s"""|load data inpath '/user/jd_ad/ads_anti/guanxinyu/metadata/hive/$table/pt=$pt/dt=$dt'|overwrite into table antidb.$table|partition(pt='$pt', dt='$dt')
         """.stripMargin
  }
  }.par

  sqls.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(4))

  sqls map spark.sql*/
}

object Task1 extends App with SimpleSpark {
  import spark.implicits._
  val t = sc.makeRDD(List("{k:1,v:2}")).toDF("value")
  t.createOrReplaceTempView("t")
//  """
//    |select *
//    |from t
//    |LATERAL VIEW json_tuple(value, k, v)
//  """.stripMargin.go show
}
