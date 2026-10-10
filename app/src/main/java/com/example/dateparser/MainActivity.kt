package com.example.dateparser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.huawei.wordnote.domain.services.dateparser.ParsedDate
import ru.alox1d.wordnote.huawei.domain.services.dateparser.DateTypes
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ParsedDate.prepareParse(applicationContext)
//        val dp = ParsedDate.parseDate( "Вернуться домой в 8:30 вечера через месяц и приготовить ужин. " +
//                "Купить молоко и помыть машину в понедельник.")
//        val dp = ParsedDate.parseDate( "Приготовить ужин завтра")
//        val dp = ParsedDate.parseDate( "Запись на стрижку в 17:00 в четверг")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку завтра в 17:00 и")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку 03.01")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку 21.22")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку 03.01.2021")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку завтра через месяц")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку в 17:00 в четверг")[0].valueOf()
//        val dp = ParsedDate.parseDate( "Запись на стрижку завтра в 17:00")[0].valueOf()


//        val dp = ParsedDate.parseDate("Haircut at 17:00 on Thursday")[0].valueOf()
//        val dp = ParsedDate.parseDate( "купить через час")[0].valueOf()
        val dp = ParsedDate.parseDate( "купить через 5 часов")[0].valueOf()
//        val dp = ParsedDate.parseDate( "сходить 1 марта")[0].valueOf()
//        val dp = ParsedDate.parseDate( "walk 1 March")[0].valueOf()
//                val dp = ParsedDate.parseDate( "сходить 1 марта")[0].valueOf()

//        val dp = ParsedDate.parseDate( "купить через 5 дней")[0].valueOf()
//        val dp = ParsedDate.parseDate( "купить через 5 дней")[0].valueOf()
//        val dp = ParsedDate.parseDate( "купить через неделю")[0].valueOf()
//        val dp = ParsedDate.parseDate( "купить через год")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "купить через 2 дня")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "купить в воскресенье")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "купить без 5 минут 6")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at five minutes past eight")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at eight o’clock")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at a quarter past eight")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at half past eight")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at a quarter to nine")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy in an hour")[0].valueOf()
//                val dp = ParsedDate.parseDate( "купить в 5 часов")[0].valueOf()
        //        val dp = ParsedDate.parseDate( "buy at 5 pm")[0].valueOf()
//                val dp = ParsedDate.parseDate( "buy at 5:45 am")[0].valueOf()
//                val dp = ParsedDate.parseDate( "buy on Sunday")[0].valueOf()
//                val dp = ParsedDate.parseDate( "buy after tomorrow")[0].valueOf()
        // НЕ работает. Почему-то
//                val dp = ParsedDate.parseDate( "КУП ЧЕРЕЗ 3 ДНЯ")[0].valueOf()

        val c: Calendar = Calendar.getInstance()
        c.timeInMillis = dp.target_date.timeInMillis

    }
}