package ru.alox1d.wordnote.huawei.domain.services.dateparser

import android.content.Context
import com.alox1d.dateparser.R
import com.alox1d.dateparser.dateparser.utils.Utils

data class Expression(var text: String, var regex_char: String, var errors_limit: Double,
                      var value: Double, var maximum: Double, var is_regex: Boolean = false, var is_separator: Boolean = false) {
//    /**@type {String} */
//    text;
//    /**@type {String} */
//    regex_char;
//    /**@type {Number} */
//    errors_limit;
//    /**@type {Number} */
//    value;
//    /**@type {Number} */
//    maximum;
    /**@type {Number} */
    var context: Int = 0;
//    /**@type {Boolean} */
//    is_separator;
//    /**@type {Boolean} */
//    is_regex;

    /**
     * @param {String} text
     * @param {String} regex_char
     * @param {Number} errors_limit
     * @param {Number} value
     * @param {Number} maximum
     * @param {Boolean} is_regex
     */
//    constructor() {
//        this.text = text;
//        this.regex_char = regex_char;
//        this.errors_limit = errors_limit;
//        this.value = value;
//        this.maximum = maximum;
//        this.is_separator = false;
//        this.is_regex = is_regex;
//    }
}

class ParsingRules(var expressions: List<Expression>) {
//    /**@type {Array.<Expression>} */
//    expressions;
//
//    /** @param {Array.<Expression>} expressions */
//    constructor(expressions) {
//        this.expressions = expressions;
//    }
}

class MonthSize(var value: Int, var normal_count: Double, var leap_count: Double) {
//    /**@type {Number} */
//    value;
//    /**@type {Number} */
//    normal_count;
//    /**@type {Number} */
//    leap_count;
//    /**
//     * @param {Number} value
//     * @param {Number} normal_count
//     * @param {Number} leap_count
//     */
//    constructor(value, normal_count, leap_count) {
//        this.value = value;
//        this.normal_count = normal_count;
//        this.leap_count = leap_count;
//    }
}

class Loader(val context: Context) {
    companion object {
        var separatorsFilename = R.raw.separators
        var monthSizesFilename = R.raw.months_size

        private var INSTANCE: Loader? = null

        fun getInstance(context: Context): Loader {
            INSTANCE = if (INSTANCE == null)  Loader(context) else INSTANCE
            return INSTANCE!!
        }
//                INSTANCE ?: synchronized(this) {
//                    INSTANCE ?: buildLoader(context).also { INSTANCE = it }
//                }

//        private fun buildLoader(context: Context) =
//                Room.databaseBuilder(context.applicationContext,
//                        UsersDatabase::class.java, "Sample.db")
//                        .build()
    }


    /**
     * Loads expressions
     * @param {String} fileName name of file containting expressions (.csv)
     * @returns {ParsingRules} Parsing Rules
     */
    fun LoadExpressionsFile(fileName: Int): ParsingRules {

        val raw = Utils.FileUtils.readRawTextFile(context, fileName)
        val rawSplitted = raw!!.split("\n").toMutableList()
//        rawSplitted.removeAt(rawSplitted.size-1)
//    val raw = fs.readFileSync(__dirname + `/expressions/${fileName}`, { encoding: 'utf-8' }).split(/\r\n/);
        val metadata = HashMap<String, String>()
        val params = rawSplitted[1].split(';');
        val values = rawSplitted[2].split(';');
        for (i in params.indices) {
            if (params[i].isNotEmpty()) {
                metadata[params[i]] = values[i];
            }
        }
        val arr = rawSplitted.slice(4 until rawSplitted.size).toMutableList()
        val arrExp = arrayOfNulls<Expression>(arr.size)
//        val arrExp: ArrayList<Expression> = arrayListOf()
//        arrExp.ensureCapacity(arr.size)
        for (i in arr.indices) {
            val value: List<String> = arr[i].split(';')
            arrExp[i] = Expression(value[0], value[1], value[2].toDouble(), value[3].toDouble(), value[4].toDouble(), value[5].toDouble() > 0)
        }
        val nonNull:ArrayList<Expression> = ArrayList(arrExp.filterNotNull())
        return ParsingRules(nonNull);
    }


    /**
     * Loads all expressions from .csv files in ./lib/expressions/
     * @returns {Array<ParsingRules>} array of expression clusters
     */
    fun LoadExpressions(): ArrayList<ParsingRules> {
//    let filenames = fs.readdirSync(__dirname + '/expressions/');
        val filenames = arrayOf(R.raw.en, R.raw.ru, R.raw.separators, R.raw.regexs) // TODO LOAD ALL RAW RESOURCES
        val parsingRules = ArrayList<ParsingRules>();
        for (filename in filenames) {
//        if (filename.split('.').pop() == 'csv' && filename != separatorsFilename) {
            if (filename != separatorsFilename) {
                parsingRules.add(LoadExpressionsFile(filename));
            }
        }
        return parsingRules;
    }

    /**
     * Loads all separating regexs from separators.csv
     * @returns {ParsingRules}
     */
    fun LoadSeparators(): ParsingRules {
        val raw = Utils.FileUtils.readRawTextFile(context, separatorsFilename)

//        val raw = fs . readFileSync (__dirname + `/expressions/${separatorsFilename}`, { encoding: 'utf-8' }).split(/\r\n/);
        val rawSplitted: List<String> = raw!!.split("\n")

        val metadata = HashMap<String, String>()
        val params = rawSplitted[1].split(';');
        val values = rawSplitted[2].split(';');
        for (i in params.indices) {
            if (params[i].isNotEmpty()) {
                metadata[params[i]] = values[i];
            }
        }
        val arr = rawSplitted.slice(4 until rawSplitted.size).toMutableList()
//        val arrExp: ArrayList<Expression> = ArrayList<Expression>()
        val arrExp = arrayOfNulls<Expression>(arr.size)

        for (i in arr.indices) {

            val value = arr[i].split(';');
            arrExp[i] = Expression(value[0], value[1], value[2].toDouble(), value[3].toDouble(), value[4].toDouble(), value[5].toDouble() > 0);
        }
        val nonNull:ArrayList<Expression> = ArrayList(arrExp.filterNotNull())
        return ParsingRules(nonNull);
    }

    /**
     * @param {Number} month
     * @returns {MonthSize}
     */
    fun LoadMonthSize(month: Int): MonthSize? {
        val raw = Utils.FileUtils.readRawTextFile(context, monthSizesFilename)

//        val raw = fs . readFileSync (__dirname + `/expressions/${separatorsFilename}`, { encoding: 'utf-8' }).split(/\r\n/);
        val rawSplitted: List<String> = raw!!.split("\n")
//        let raw = fs . readFileSync (__dirname + `/constants/${monthSizesFilename}`, { encoding: 'utf-8' }).split(/\r\n/);
        val arr = rawSplitted.slice(1 until rawSplitted.size);
        val arrExp = arrayOfNulls<Expression>(arr.size)
        for (i in arr.indices) {

            val value = arr[i].split(';');
            if ((value[0].toInt()) == month) {
                return MonthSize(value[0].toInt(), value[1].toDouble(), value[2].toDouble());
            }
        }
        return null;
    }
}
